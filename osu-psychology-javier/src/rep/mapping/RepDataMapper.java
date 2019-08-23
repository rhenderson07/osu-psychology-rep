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
import java.util.stream.Stream;

import rep.mapping.domain.Participant;
import rep.mapping.domain.Session;

public class RepDataMapper {

	private final String sourceRepDataFilePathStr;
	private final String destRepDataFilePathStr;

	private static final String TREATMENT_SESSION_CODE = "SRF1R1";
	private static final String NULL_VAL = "NULL";
	
	private final LocalDate firstDate;
	private final LocalDate lastDate;

	public RepDataMapper(String srcPath, String destPath, LocalDate startDate, LocalDate endDate) {
		sourceRepDataFilePathStr = srcPath;
		destRepDataFilePathStr = destPath;
		firstDate = startDate;
		lastDate = endDate;
	}

	
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
		double cumulativeCreditsMissed = 0.0;
		boolean hasRecievedTreatment = false;

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

			double creditsEarnedToday = sum(sessionsToday.stream().map(Session::getCreditsEarned));
			outputRecord.setCreditsEarned(creditsEarnedToday);
			cumulativeCreditsEarned += creditsEarnedToday;
			outputRecord.setCumulativeCreditsEarned(cumulativeCreditsEarned);			
			

			outputRecord.setCancelCount(sessionsToday.stream().filter(Session::isCancelled).count());
			outputRecord.setMissedCount(sessionsToday.stream().filter(Session::isMissed).count());
			double creditsMissedToday = sum(sessionsToday.stream().map(Session::getCreditsMissed));
			outputRecord.setCreditsMissed(creditsMissedToday);
			cumulativeCreditsMissed += creditsMissedToday;
			outputRecord.setCumulativeCreditsMissed(cumulativeCreditsMissed);
			
			
			hasRecievedTreatment = hasRecievedTreatment || sessionsToday.stream().filter(Session::isAttended).anyMatch(Session::isTreated);
			outputRecord.setRecievedTreatment(hasRecievedTreatment);

			daySummaries.add(outputRecord);
		}

		return daySummaries;
	}

	private double sum(Stream<Double> numberStream) {
		return numberStream //
				.filter(Objects::nonNull) //
				.reduce(0.0, (a, b) -> a + b);
	}

	private OriginalRecord parseOriginalRecord(String originalRecordStr) {
		String[] fields = originalRecordStr.split(",");

		OriginalRecord record = new OriginalRecord();

		record.setId(fields[0]);
		record.setCreditMissed(parseDouble(fields[1]));
		record.setCreditEarned(parseDouble(fields[2]));
		record.setCancelDateTime(parseDateTime(fields[3]));
		record.setEnrollDateTime(parseDateTime(fields[4]));
		record.setExperimentId(fields[5]);
		record.setSessionCode(fields[6]);
		record.setSessionDate(parseDate(fields[7]));
		record.setStartTime(parseTime(fields[8]));

		return record;
	}
	
	private Double parseDouble(String doubleStr) {
		return NULL_VAL.equals(doubleStr) ? null : Double.parseDouble(doubleStr);
	}

	private LocalDate parseDate(String dateStr) {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yy");
		return NULL_VAL.equals(dateStr) ? null : LocalDate.parse(dateStr, dateFormatter);
	}

	private LocalDateTime parseDateTime(String dateTimeStr) {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yy H:mm");
		return NULL_VAL.equals(dateTimeStr) ? null : LocalDateTime.parse(dateTimeStr, dateFormatter);
	}

	private LocalTime parseTime(String timeStr) {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
		return NULL_VAL.equals(timeStr) ? null : LocalTime.parse(timeStr, dateFormatter);
	}
	
	private String asString(boolean bool) {
		return bool ? "1" : "0";
	}

	private static final String OUTPUT_RECORDS_HEADERS = "participantId,dayNum,date,enrolledCount,participatedCount,creditsEarned,cumulativeCreditsEarned,recievedTreatment,cancelCount,missedCount,creditsMissed,cumulativeCreditsMissed";
	
	private String formatOutput(OutputRecord outputRecord) {
		StringBuilder sb = new StringBuilder("");

		sb.append(outputRecord.getParticipantId() + ",");
		sb.append(outputRecord.getDay() + ",");
		sb.append(outputRecord.getDate() + ",");
		sb.append(outputRecord.getEnrolledCount() + ",");
		sb.append(outputRecord.getParticipatedCount() + ",");
		sb.append(outputRecord.getCreditsEarned() + ",");
		sb.append(outputRecord.getCumulativeCreditsEarned() + ",");
		sb.append(asString(outputRecord.isRecievedTreatment())+ ",");
		sb.append(outputRecord.getCancelCount() + ",");

		sb.append(outputRecord.getMissedCount() + ",");
		sb.append(outputRecord.getCreditsMissed() + ",");
		sb.append(outputRecord.getCumulativeCreditsMissed());

		return sb.toString();
	}

	private List<Participant> mapToParticipants(List<String> repRecordStrs) {
		List<OriginalRecord> originalRecords = repRecordStrs.stream() //
				.map(this::parseOriginalRecord) //
				.collect(Collectors.toList());

		Map<String, List<OriginalRecord>> participantIdToRecordMap = originalRecords.stream() //
				.collect(Collectors.groupingBy(OriginalRecord::getId));

		return participantIdToRecordMap.entrySet().stream() //
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
		
		session.setMissed(originalRecord.getCreditMissed() != null && originalRecord.getCreditMissed() > 0);
		session.setCreditsMissed(originalRecord.getCreditMissed());
		
		session.setTreated(TREATMENT_SESSION_CODE.equals(originalRecord.getSessionCode()));

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

		String sourcePath = "C:\\Users\\Randy\\Desktop\\osu-psych\\Javier data 2019\\REP-SP19-enrollments-PH.csv";
		String destPath = "C:\\Users\\Randy\\Desktop\\osu-psych\\Javier data 2019\\REP-SP19-enrollments-PH - restructured.csv";
		LocalDate firstDate = LocalDate.of(2019, 1, 7);
		LocalDate lastDate = LocalDate.of(2019, 4, 16);

		RepDataMapper mapper = new RepDataMapper(sourcePath, destPath, firstDate, lastDate);
		mapper.run();

		System.out.println("Complete");
	}
}
