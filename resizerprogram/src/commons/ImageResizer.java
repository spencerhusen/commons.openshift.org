package commons;

import java.awt.Graphics2D;
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

	/**
	 * Specific text String located in cURL command output on previous line as extracted body
	 * information
	 */
	private static final String BODY_FLAG = "author_association";
	
	/**
	 * System's COMMONS_PATH environmental variable, which leads to the root directory
	 * of the project's GitHub repo
	 */
	private static final String COMMONS_PATH = System.getenv("COMMONS_PATH");
	
	/** Number of pieces of information extracted from each GitHub issue */
	private static final int ISSUE_DATA = 3;
	
	/** Specific text String located in cURL command output on same line as body of Issues */
	private static final String ISSUE_FLAG = "Company";
	
	/** Specific index within 'body' line where end of ISSUE_FLAG may exist */
	private static final int ISSUE_FLAG_END = 16;
	
	/** Specific index within 'body' line where start of ISSUE_FLAG may exist */
	private static final int ISSUE_FLAG_START = 9;

	/** String located in GitHub issues for any piece of information*/
	private static final String ISSUE_SKIP_FLAG = " TBD";
	
	/** Maximum pixel width for uploaded company logo */
	private static final float MAX_HEIGHT = 60;
	
	/**
	 * Regular expression pattern used to capture company name, url, and logo url from
	 * cURL output
	 */
	private static final String REGEX_PATTERN = "(?<= )[^\\\\\"]++";
	
	/**
	 * Discretionary number of milliseconds the copyURLToFile method will run until timeout
	 */
	private static final int TIMEOUT_MILLIS = 10000;

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
		boolean addIssue = true;
		String company = null;
		String url =  null;
		String link = null;
		boolean companiesAdded = false;

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
		 * The main functionality of the program; processes through the GitHub Issue looking for
		 * specific Strings of text that indicate whether or not the Issue involves adding a
		 * company to the OpenShift Commons participants. If the Issue is formatted correctly and
		 * the company has not already been processed, the program appends the company to the
		 * 'participants.yml' file at the end of the list and calls on the proper method to
		 * process and resize the image file at the URL stored in the "link" String
		 */
		while (inputReader.hasNextLine()) {
			line = inputReader.nextLine();
			if (line.contains(BODY_FLAG)) {
				bodyLine = inputReader.nextLine().trim();
				if (bodyLine.substring(ISSUE_FLAG_START, ISSUE_FLAG_END)
						.equalsIgnoreCase(ISSUE_FLAG)) {
					Matcher m = Pattern.compile(REGEX_PATTERN).matcher(bodyLine);
					//Uses custom generated regex pattern to capture and store specific Strings
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
					
					//Checks for duplicate and checks to see if any field is "TBD"
					while (participantsReader.hasNextLine()) {
						ymlLine = participantsReader.nextLine();
						if ((ymlLine.contains(company)) || (bodyLine.contains(ISSUE_SKIP_FLAG))) {
							addIssue = false;
						}
					}
					participantsReader.close();
					
					if (addIssue) {
						String extension = getExtension(link);
						//If SVG file, does not resize image and vice-a-versa
						if (extension.equals("svg")) {
							resizeSVG(company, link);
						} else {
							resizeNonSVG(company, link, extension);
						}
						companiesAdded = true;
						//Appends company and information to 'participants.yml'
						out.append("- name: \"" + company + "\"");
						out.newLine();
						out.append("  link: \"" + url + "\"");
						out.newLine();
						out.append("  logo: \"commons-logos/" +
								company.toLowerCase().replaceAll("\\s","") + "." + extension + "\"");
						out.newLine();
						//Prints to the console which companies were added
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
		if (!companiesAdded) {
			System.out.println("No new companies added.\n");
		}
		out.close();
	}

	/**
	 * Static method simply responsible for determining the file extension of the picture file
	 * going to be used as the company's logo on the website
	 * @param logo the url of the image thats extension is to be determined
	 * @return the file extension of the image at the URL
	 */
	public static String getExtension(String img) {
		return img.substring(img.lastIndexOf(".") + 1);
	}

	/**
	 * Static method used to process the image at the parameterized URL, retrieve its dimensions,
	 * and resize the image if the height of the image exceeds 60 pixels. If the height is above
	 * the max height, creates new BufferedImage file and uses Graphics2D class to redraw the
	 * graphics from oversized logo onto new, smaller "canvas" file. Then, new file is output into
	 * commons-logos with the proper name. If not oversized, simply outputs logo.
	 * @param company the name of the company whose logo is to be processed
	 * @param logoUrl the URL that contains the company's logo image file
	 * @param extension the logo image's type (file extension)
	 */
	public static void resizeNonSVG(String company, String logoUrl, String extension) {
		//Using the ImageIO and URL classes, reads in the image at the given URL
		BufferedImage logo = null;
		URL url;
		try {
			url = new URL(logoUrl);
			logo = ImageIO.read(url);
		} catch (IOException e) {
			System.out.println("Error: Unable to read the image at the specified URL");
		}
		
		//Initializes dimensional variables for input logo image and whether image was resized
		int height = logo.getHeight();
		int width = logo.getWidth();
		int type = logo.getType();
		boolean resized = false;
		BufferedImage outputLogo = null;
		//Calculates scale-down ratio
		float conversionRatio = MAX_HEIGHT / (float) height;
		
		/**
		 * Re-assigns width and height variables to properly scale logo dimensions and draws logo
		 * image onto newly resized "blank canvas" IF height exceeds maximum height
		 */
		if (height > MAX_HEIGHT) {
			resized = true;
			width = (int) (width * conversionRatio);
			height = (int) (height * conversionRatio);
			outputLogo = new BufferedImage(width, height, type);
			Graphics2D imageDrawer = outputLogo.createGraphics();
			imageDrawer.drawImage(logo, 0, 0, width, height, null);
			imageDrawer.dispose();
		}
		
		//Writes the new, resized image to its proper location in the GitHub repo
		File outputFile = new File(COMMONS_PATH + "/source/img/commons-logos/" +
				company.toLowerCase().replaceAll("\\s","") + "." + extension);
		//If image was not resized, write input (unaltered) image
		if (!resized) {
			try {
				ImageIO.write(logo, extension, outputFile);
			} catch (IOException e) {
				System.out.println("Error: Unable to write the image to the specified location");
			}
		//If image was resized, write output (altered) image
		} else {
			try {
				ImageIO.write(outputLogo, extension, outputFile);
			} catch (IOException e) {
				System.out.println("Error: Unable to write the image to the specified location");
			}
		}
	}

	/**
	 * Static method used to process Scalable Vector Graphic image files. Method simply writes SVG
	 * file at parameterized link to commons-logos
	 * @param company
	 * @param logoUrl
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