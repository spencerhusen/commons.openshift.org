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

	/** Specific text String located in cURL command output on same line as number of Issues */
	public static final String ISSUE_COUNT_FLAG = "number";
	/** Specific index within 'number' line in cURL output noting how many Issues exist in repo */
	public static final int ISSUE_COUNT_LOCATION = 10;
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
	public static final int MAX_WIDTH = 120;
	/** */
	public static final String COMMONS_PATH = System.getenv("COMMONS_PATH");
	
	/**
	 * Main method; contains most critical functionality of program including establishing
	 * Scanner for piped input and extracting critical body information from cURL output
	 * @param args command line arguments
	 * @throws IOException if the appending operation fails
	 */
	public static void main(String[] args) throws IOException {
		
		//Establishes input Scanners used to read piped-in cURL output
		Scanner inputReader = new Scanner(System.in);
		
		/**
		 * Following 5 lines declare/initialize String variables necessary for properly parsing
		 * text input and extracting company name, url, and logo url
		 */
		String line;
		String bodyLine;
		String company = null;
		String link =  null;
		String logo = null;
		
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
		 * While traversing each line of input file, stops at each line containing
		 * "author association," which will always be located in line before important body
		 * information. Once there, program uses a regular expression pattern to extract the
		 * company name, url, and logo url and stores the information in a 2D array
		 */
		while (inputReader.hasNextLine()) {
			line = inputReader.nextLine();
			if (line.contains(BODY_FLAG)) {
				bodyLine = inputReader.nextLine().trim();
				if (bodyLine.substring(9, 16).equalsIgnoreCase("Company")) {
					Matcher m = Pattern.compile(REGEX_PATTERN).matcher(bodyLine);
					for (int i = 0; i < ISSUE_DATA; i++) {
						if (m.find()) {
							//TODO Fix matching mechanism
							company = m.group(0);
							link = m.group();
							logo = m.group();
						}
					}
					System.out.println(company);
					System.out.println(link);
					System.out.println(logo);
					resizeImage(company, logo);
					out.append("- name: \"" + company + "\"");
					out.newLine();
					out.append("  link: \"" + link + "\"");
					out.newLine();
					out.append("  logo: \"commons-logos/" + company.toLowerCase().replaceAll("\\s","") + ".png");
					out.newLine();
				}
			}
		}
		inputReader.close();
		
		//Closes FileWriter
		out.close();
	}
	
	/**
	 * Void method responsible for processing each company's logo given its URL, properly resizing
	 * it, and outputting it to the GitHub repo in its correct location
	 * @param logoUrl the String representing the URL of where each company's logo is located online
	 */
	public static void resizeImage(String company, String logoUrl) {
		BufferedImage logo = null;
		try {
			URL url = new URL(logoUrl);
			logo = ImageIO.read(url);
		} catch (IOException e) {
			System.out.println("Unable to read the image at the specified URL");
		}
		//TODO Resizing mechanism
		File outputLogo = new File(COMMONS_PATH + "/source/img/commons-logos/" + company.toLowerCase().replaceAll("\\s","") + ".png");
		try {
			ImageIO.write(logo, "png", outputLogo);
		} catch (IOException e) {
			System.out.println("Unable to write the image to the specified path");
		}
	}
	
}