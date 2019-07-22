package rep.mapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import rep.mapping.domain.Participant;
import rep.mapping.domain.Session;

public class RepDataMapper {

	private final String sourceRepDataFilePathStr;
	private final String destRepDataFilePathStr;

	private static final String NULL_VAL = "\\N";

	private final LocalDate firstDate;
	private final LocalDate lastDate;

	public RepDataMapper(String srcPath, String destPath, LocalDate startDate, LocalDate endDate) {
		sourceRepDataFilePathStr = srcPath;
		destRepDataFilePathStr = destPath;
		firstDate = startDate;
		lastDate = endDate;
	}

	private static final String OUTPUT_RECORDS_HEADERS = "participantId,dayNum,date,enrolledCount,participatedCount,creditsEarned,cumulativeCreditsEarned";

	private List<String> readSourceData(Path srcDataPath) {
		List<String> originalRecords = Collections.emptyList();
		try {
			originalRecords = Files.readAllLines(srcDataPath);
			originalRecords.remove(0); // ignore labels
		} catch (IOException e) {
			System.out.println("unable to read file: \n" + e.getMessage());
		}
		return originalRecords;
	}

	private void outputParticipantData(Collection<String> outputStrings, Path destDataPath) {
		try {
			Files.write(destDataPath, outputStrings);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<OutputRecord> generateDaySummaries(Participant paticipant) {
		List<OutputRecord> daySummaries = new ArrayList<>();

		double cumulativeCreditsEarned = 0.0;

		for (LocalDate d = firstDate; !d.isAfter(lastDate); d = d.plusDays(1)) {
			OutputRecord outputRecord = new OutputRecord();

			outputRecord.setDate(d);
			outputRecord.setDay((int) ChronoUnit.DAYS.between(firstDate, d) + 1);
			outputRecord.setParticipantId(paticipant.getId());

			List<Session> sessionsToday = paticipant.getSessions().stream() //
					.filter(x -> x.getSessionDate().equals(outputRecord.getDate())) //
					.collect(Collectors.toList());

			outputRecord.setEnrolledCount(sessionsToday.stream().filter(Session::isEnrolled).count());
			outputRecord.setParticipatedCount(sessionsToday.stream().filter(Session::isAttended).count());

			double creditsEarnedToday = sumCreditsForSessions(sessionsToday);
			outputRecord.setCreditsEarned(creditsEarnedToday);
			cumulativeCreditsEarned += creditsEarnedToday;
			outputRecord.setCumulativeCreditsEarned(cumulativeCreditsEarned);

			daySummaries.add(outputRecord);
		}

		return daySummaries;
	}

	private double sumCreditsForSessions(Collection<Session> sessions) {
		return sessions.stream() //
				.map(Session::getCreditsEarned) //
				.filter(Objects::nonNull) //
				.reduce(0.0, (a, b) -> a + b);
	}

	private OriginalRecord parseOriginalRecord(String originalRecordStr) {
		String[] fields = originalRecordStr.split(",");

		OriginalRecord record = new OriginalRecord();

		record.setId(parseInt(fields[0]));
		record.setEmail(fields[1]);
		record.setDebit(parseDouble(fields[2]));
		record.setCreditEarned(parseDouble(fields[3]));
		record.setCancelDateTime(parseDateTime(fields[4]));
		record.setEnrollDateTime(parseDateTime(fields[5]));
		record.setExperimentId(fields[6]);
		record.setCode(fields[7]);
		record.setSessionDate(parseDate(fields[8]));
		record.setStartTime(parseTime(fields[9]));
		// ignore col 10 for REP_start
		record.setEventDateOffset(parseInt(fields[11]));

		return record;
	}
	
	private Integer parseInt(String intStr) {
		return NULL_VAL.equals(intStr) ? null : Integer.parseInt(intStr);
	}
	
	private Double parseDouble(String doubleStr) {
		return NULL_VAL.equals(doubleStr) ? null : Double.parseDouble(doubleStr);
	}

	private LocalDate parseDate(String dateStr) {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy");
		return NULL_VAL.equals(dateStr) ? null : LocalDate.parse(dateStr, dateFormatter);
	}

	private LocalDateTime parseDateTime(String dateTimeStr) {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yy H:mm");
		return NULL_VAL.equals(dateTimeStr) ? null : LocalDateTime.parse(dateTimeStr, dateFormatter);
	}

	private LocalTime parseTime(String timeStr) {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("h:mm:ss a");
		return NULL_VAL.equals(timeStr) ? null : LocalTime.parse(timeStr, dateFormatter);
	}

	private String formatOutput(OutputRecord outputRecord) {
		StringBuilder sb = new StringBuilder("");

		sb.append(outputRecord.getParticipantId() + ",");
		sb.append(outputRecord.getDay() + ",");
		sb.append(outputRecord.getDate() + ",");
		sb.append(outputRecord.getEnrolledCount() + ",");
		sb.append(outputRecord.getParticipatedCount() + ",");
		sb.append(outputRecord.getCreditsEarned() + ",");
		sb.append(outputRecord.getCumulativeCreditsEarned());

		return sb.toString();
	}

	private List<Participant> mapToParticipants(List<String> repRecordStrs) {
		List<OriginalRecord> originalRecords = repRecordStrs.stream() //
				.map(this::parseOriginalRecord) //
				.collect(Collectors.toList());

		Map<String, List<OriginalRecord>> emailToRecordMap = originalRecords.stream() //
				.collect(Collectors.groupingBy(OriginalRecord::getEmail));

		return emailToRecordMap.entrySet().stream() //
				.map(x -> new Participant(x.getKey(), mapToSessions(x.getValue()))) //
				.sorted(Comparator.comparing(Participant::getId)) //
				.collect(Collectors.toList());
	}

	private List<Session> mapToSessions(Collection<OriginalRecord> origionalRecords) {
		return origionalRecords.stream()//
				.map(this::mapToSession)//
				.collect(Collectors.toList());
	}

	private Session mapToSession(OriginalRecord originalRecord) {
		Session session = new Session();

		session.setEnrolled(originalRecord.getEnrollDateTime() != null);
		session.setCreditsEarned(originalRecord.getCreditEarned());
		session.setAttended(originalRecord.getCreditEarned() != null && originalRecord.getCreditEarned() > 0);
		session.setSessionDate(originalRecord.getSessionDate());
		session.setCancelled(originalRecord.getCancelDateTime() != null);

		return session;
	}

	public void run() {

		Path srcDataPath = Paths.get(sourceRepDataFilePathStr);
		System.out.println("Reading data from source file: " + srcDataPath);

		List<String> originalRecordStrs = readSourceData(srcDataPath);

		List<Participant> participants = mapToParticipants(originalRecordStrs);

		List<OutputRecord> outputRecords = participants.stream() //
				.map(this::generateDaySummaries) //
				.flatMap(List::stream) //
				.collect(Collectors.toList());

		List<String> outputLines = outputRecords.stream()//
				.map(this::formatOutput)//
				.collect(Collectors.toList());
		
		outputLines.add(0, OUTPUT_RECORDS_HEADERS);

		Path destDataPath = Paths.get(destRepDataFilePathStr);
		System.out.println("Writing output to dest file: " + destDataPath);

		outputParticipantData(outputLines, destDataPath);
	}

	public static void main(String[] args) {
		System.out.println("Begin ...");

		String sourcePath = "C:\\Users\\Randy\\Desktop\\osu-psych\\Javier data 2018\\Fazio Data AU2017 - Student Data.csv";
		String destPath = "C:\\Users\\Randy\\Desktop\\osu-psych\\Javier data 2018\\restructured-REP-data.csv";
		LocalDate firstDate = LocalDate.of(2017, 8, 22);
		LocalDate lastDate = LocalDate.of(2017, 12, 7);

		RepDataMapper mapper = new RepDataMapper(sourcePath, destPath, firstDate, lastDate);
		mapper.run();

		System.out.println("Complete");
	}
}
