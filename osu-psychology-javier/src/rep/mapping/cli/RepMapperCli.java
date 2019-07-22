package rep.mapping.cli;

import java.time.LocalDate;

import rep.mapping.RepDataMapper;

public class RepMapperCli {
	public static void main(String[] args) {
		System.out.println("Begin ...");

		String sourcePath = args[0];
		String destPath = args[1];
		LocalDate firstDate = LocalDate.parse(args[2]);
		LocalDate lastDate = LocalDate.parse(args[3]);

		RepDataMapper mapper = new RepDataMapper(sourcePath, destPath, firstDate, lastDate);
		mapper.run();

		System.out.println("Complete");
	}
}
