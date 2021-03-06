package org.poseidon;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Tracker {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	@Parameter(description = "localtion of the video file or ipcamaddress")
	private String video;

	@Parameter(description = "max number of results.")
	private int maxResults = 10;
	private JPanel panelCamera = new JPanel();
	private JFrame frameCamera = createFrame("Camera", panelCamera);
	private JFrame frameThreshold;
	//max number of objects to be detected in frame
		private final int MAX_NUM_OBJECTS = 50;
		
		//minimum and maximum object area
		private final int MIN_OBJECT_AREA = 40 * 40;
       //Ignore the image border
		private double MIN_X_BORDER=10;
		private double MIN_Y_BORDER=10;
	public void startTracking() throws Exception {

		Mat image = new Mat();
		Mat thresholdedImage = new Mat();
		Mat hsvImage = new Mat();
		VideoCapture capture = null;
		if (video == null) {
			capture = new VideoCapture(0);
		} else {
			capture = new VideoCapture(video);

		}
		if (capture == null){
			throw new Exception("Could not conect to camera.");
		}
		// Captures one image, for starting the process.
				try{
					capture.read(image);
				} catch (Exception e){
					throw new Exception("Could not read from camera. Maybe the URL is not correct.");
				}
				 
				setFramesSizes(image);
				
				if (capture.isOpened()) {
					
					while (true) {
						int count=0;
						capture.read(image);
						
						if (!image.empty()) {
							Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_BGR2HSV);
							Imgproc.GaussianBlur(hsvImage,hsvImage,new Size(21, 21), 0);							
							Imgproc.threshold(hsvImage, hsvImage, 25, 255, Imgproc.THRESH_BINARY);
							Imgproc.dilate(hsvImage, hsvImage, null,new Point(-1,-1),2);
							
							List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
							Mat temp = new Mat();
							Mat hierarchy = new Mat();
							hsvImage.copyTo(temp);
							Imgproc.findContours(temp, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
							if (contours.size() > 0) {
								int numObjects = contours.size();
						
								//if number of objects greater than MAX_NUM_OBJECTS we have a noisy filter
								if (numObjects < MAX_NUM_OBJECTS) {
						
									for (int i=0; i< contours.size(); i++){
										Moments moment = Imgproc.moments(contours.get(i));
										double area = moment.get_m00();
						
										//if the area is less than 20 px by 20px then it is probably just noise
										//if the area is the same as the 3/2 of the image size, probably just a bad filter
										//we only want the object with the largest area so we safe a reference area each
										//iteration and compare it to the area in the next iteration.
										if (area > MIN_OBJECT_AREA) {
											Point centroid = new Point();
											centroid.x = moment.get_m10() / moment.get_m00();
											centroid.y = moment.get_m01() / moment.get_m00();
											if(centroid.x>MIN_X_BORDER && centroid.x<temp.size().width-MIN_X_BORDER &&
													centroid.y>MIN_Y_BORDER && centroid.y<temp.size().height-MIN_Y_BORDER	)
											{
												count++;
											}
											else//some is coming
											{
												
											}

										}
									}
								}
							}
										
						}
						else{
					}
				}

	}
	}

	private JFrame createFrame(String frameName, JPanel panel) {
		JFrame frame = new JFrame(frameName); 	
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(640, 480);
		frame.setBounds(0, 0, frame.getWidth(), frame.getHeight());		
		frame.setContentPane(panel);
		frame.setVisible(true);
		return frame;
	}

	private void setFramesSizes(Mat image) {
		frameCamera.setSize(image.width() + 20, image.height() + 60);
		
		
		
	}

	public static void main(String[] args) throws Exception {
		Tracker tracker = new Tracker();
		new JCommander(tracker, args);
		tracker.startTracking();
	}
}
