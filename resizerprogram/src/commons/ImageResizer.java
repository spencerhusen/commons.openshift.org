package commons;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.*;

import javax.imageio.ImageIO;

/**
 * Java program responsible for extracting necessary information from cURL command, resizing
 * and re-uploading the picture at the provided link to the correct size, and appending the new
 * information/file to the 'participants.yml' file within the commons.openshift.org GitHub repo
 * 
 * @author shusen - Summer 2018
 *
 */
public class ImageResizer {

	/** Specific text String located in cURL command output on same line as body of Issues */
	public static final String ISSUE_FLAG = "Company";
	/** Specific index within 'body' line where start of ISSUE_FLAG may exist */
	public static final int ISSUE_FLAG_START = 9;
	/** Specific index within 'body' line where end of ISSUE_FLAG may exist */
	public static final int ISSUE_FLAG_END = 16;
	/** Number of pieces of information extracted from each GitHub issue */
	public static final int ISSUE_DATA = 3;
	/**
	 * Specific text String located in cURL command output on previous line as extracted body
	 * information
	 */
	public static final String BODY_FLAG = "author_association";
	/**
	 * Regular expression pattern used to capture company name, url, and logo url from
	 * cURL output
	 */
	public static final String REGEX_PATTERN = "(?<= )[^\\\\\"]++";
	/** Maximum pixel width for uploaded company logo */
	public static final int MAX_HEIGHT = 60;
	/**
	 * System's COMMONS_PATH environmental variable, which leads to the root directory
	 * of the project's GitHub repo
	 */
	public static final String COMMONS_PATH = System.getenv("COMMONS_PATH");
	
	/**
	 * Main method; contains most critical functionality of program including establishing
	 * Scanner for piped input and extracting critical body information from cURL output
	 * @param args command line arguments
	 * @throws IOException if the reading/writing operation fails
	 */
	public static void main(String[] args) throws IOException {
		
		//Establishes input Scanners used to read piped-in cURL output and 'participants.yml' file
		Scanner inputReader = new Scanner(System.in);
		
		/**
		 * Following lines declare/initialize variables necessary for properly parsing
		 * text input and extracting company name, url, and logo url
		 */
		String line;
		String bodyLine;
		String ymlLine;
		boolean duplicate = false;
		String company = null;
		String url =  null;
		String link = null;
		int companiesAdded = 0;
		
		//Establishes the BufferedWriter needed to append text to 'participants.yml'
		BufferedWriter out = null;
		File f = new File(COMMONS_PATH + "/data/participants.yml");
		try {
			out = new BufferedWriter(new FileWriter(f, true));
		} catch (IOException e) {
			System.out.println("Error: Could not write to specified file");
			e.printStackTrace();
		}
		
		/**
		 * TODO this Javadoc
		 */
		while (inputReader.hasNextLine()) {
			line = inputReader.nextLine();
			if (line.contains(BODY_FLAG)) {
				bodyLine = inputReader.nextLine().trim();
				if (bodyLine.substring(ISSUE_FLAG_START, ISSUE_FLAG_END)
						.equalsIgnoreCase(ISSUE_FLAG)) {
					Matcher m = Pattern.compile(REGEX_PATTERN).matcher(bodyLine);
					for (int i = 0; i < ISSUE_DATA; i++) {
						if (m.find()) {
							if (i == 0) {
								company = m.group();
							}
							else if (i == 1) {
								url = m.group();
							} else {
								link = m.group();
							}
						}
					}
					Scanner participantsReader = new Scanner(new File(COMMONS_PATH
							+ "/data/participants.yml"));
					while (participantsReader.hasNextLine()) {
						ymlLine = participantsReader.nextLine();
						if (ymlLine.contains(company)) {
							duplicate = true;
						}
					}
					participantsReader.close();
					if (!duplicate) {
						String extension = getExtension(link);
						resizeImage(company, link);
						companiesAdded++;
						out.append("- name: \"" + company + "\"");
						out.newLine();
						out.append("  link: \"" + url + "\"");
						out.newLine();
						out.append("  logo: \"commons-logos/" + 
								company.toLowerCase().replaceAll("\\s","") + extension + "\"");
						out.newLine();
						System.out.println("\nCompany \"" + company + "\" added.");
					}
				}
			}
		}
		
		/**
		 * Empty print line for console clarity, closes the inputReader, prints short message
		 * if no new participants were appended to 'participants.yml', and closes the
		 * BufferedWriter
		 */
		inputReader.close();
		System.out.println();
		if (companiesAdded == 0) {
			System.out.println("No new companies added.\n");
		}
		out.close();
	}
	
	/**
	 * Static method simply responsible for determining the file extension of the picture file
	 * going to be used as the company's logo on the website
	 * @param logo the url of the image thats extension is to be determined
	 * @return the proper extension of the file
	 */
	public static String getExtension(String img) {
		if (img.substring(img.length() - 5).equals(".jpeg")) {
			return ".jpeg";
		} else {
			return img.substring(img.length() - 4);
		}
	}
	
	/**
	 * Void method responsible for processing each company's logo given its URL, properly resizing
	 * it, and outputting it to the GitHub repo in its correct location
	 * @param logoUrl the String representing the URL of where the company's logo is located
	 */
	public static void resizeImage(String company, String logoUrl) {
		String extension;
		if (logoUrl.substring(logoUrl.length() - 5).equals(".jpeg")) {
			extension = ".jpeg";
		} else {
			extension = logoUrl.substring(logoUrl.length() - 4);
		}
		BufferedImage logo = null;
		try {
			URL url = new URL(logoUrl);
			logo = ImageIO.read(url);
		} catch (IOException e) {
			System.out.println("Error: Unable to read the image at the specified URL");
		}
		//TODO Resizing mechanism
		System.out.println(logo.getHeight());
		System.out.println(logo.getWidth());
		File outputLogo = new File(COMMONS_PATH + "/source/img/commons-logos/" + 
				company.toLowerCase().replaceAll("\\s","") + extension);
		try {
			ImageIO.write(logo, extension, outputLogo);
		} catch (IOException e) {
			System.out.println("Error: Unable to write the image to the specified path");
		}
	}
	
}