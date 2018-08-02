package commons;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.*;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

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

	public static final float MAX_HEIGHT = 60;
	/**
	 * System's COMMONS_PATH environmental variable, which leads to the root directory
	 * of the project's GitHub repo
	 */
	public static final String COMMONS_PATH = System.getenv("COMMONS_PATH");
	/**
	 * Discretionary number of milliseconds the copyURLToFile method will run until timeout
	 */
	public static final int TIMEOUT_MILLIS = 10000;

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
						if (extension.equals(".svg")) {
							resizeSVG(company, link);
						} else {
							resizeNonSVG(company, link, extension);
						}
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
		return img.substring(img.lastIndexOf("."));
	}

	/**
	 * TODO this Javadoc
	 */
	public static void resizeNonSVG(String company, String logoUrl, String extension) {
		//Using the ImageIO and URL classes, reads in the image at the given URL and stores H & W
		BufferedImage logo = null;
		URL url;
		try {
			url = new URL(logoUrl);
      System.out.println(url);
			logo = ImageIO.read(url);
		} catch (IOException e) {
			System.out.println("Error: Unable to read the image at the specified URL");
		}
		int height = logo.getHeight();
		int width = logo.getWidth();
		int type = logo.getType();
    float conversion_ratio = MAX_HEIGHT / (float) height;
    // System.out.println(height);
    // System.out.println(width);
    // System.out.println(type);
    // System.out.println(conversion_ratio);
    // System.out.println((int) (width *  conversion_ratio));
    // System.out.println((int) (height *  conversion_ratio));

		//Resizes image to its proper dimensions if height exceeds maximum allowed
		if (height > MAX_HEIGHT) {
			logo = new BufferedImage(
        ((int) (width *  conversion_ratio)),
				((int) (height *  conversion_ratio)),
        type
      );
		}

		//Writes the new, resized image to its proper location in the GitHub repo
		File outputLogo = new File(COMMONS_PATH + "/source/img/commons-logos/" +
				company.toLowerCase().replaceAll("\\s","") + extension);
		try {
			ImageIO.write(logo, extension, outputLogo);
		} catch (IOException e) {
			System.out.println("Error: Unable to write the image to the specified location");
		}
	}

	/**
	 * TODO this Javadoc
	 */
	public static void resizeSVG(String company, String logoUrl) {
		URL url = null;
		try {
			url = new URL(logoUrl);
		} catch (MalformedURLException e) {
			System.out.println("Error: Invalid URL");
			e.printStackTrace();
		}
		File svgFile = new File(COMMONS_PATH + "/source/img/commons-logos/" +
				company.toLowerCase().replaceAll("\\s","") + ".svg");
		try {
			FileUtils.copyURLToFile(url, svgFile, TIMEOUT_MILLIS, TIMEOUT_MILLIS);
		} catch (IOException e) {
			System.out.println("Error: Unable to write the image to the specified location");
			e.printStackTrace();
		}
	}

}
