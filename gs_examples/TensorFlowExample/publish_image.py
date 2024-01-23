#!/usr/bin/env python

import rospy
from sensor_msgs.msg import CompressedImage
import cv2
import sys
from cv_bridge import CvBridge

def image_publisher(file_path, topic_name='/mgt/img_sampler/dock_cam/image_record/compressed', publish_rate=5):
    # Initialize the ROS node
    rospy.init_node('image_publisher', anonymous=True)
    
    # Create a publisher for the specified CompressedImage topic
    image_publisher = rospy.Publisher(topic_name, CompressedImage, queue_size=10)

    # Create a CvBridge to convert between OpenCV images and ROS messages
    bridge = CvBridge()

    # Read the image from the file
    image = cv2.imread(file_path, cv2.IMREAD_GRAYSCALE)

    # Compress the image using JPEG encoding
    _, compressed_image = cv2.imencode('.jpg', image)

    # Create a CompressedImage message and set its data
    compressed_msg = CompressedImage()
    compressed_msg.header.stamp = rospy.Time.now()
    compressed_msg.format = 'jpeg'
    compressed_msg.data = compressed_image.tobytes()

    # Set the publishing rate
    rate = rospy.Rate(publish_rate)

    # Publish the compressed image at the specified rate
    while not rospy.is_shutdown():
        image_publisher.publish(compressed_msg)
        rate.sleep()

if __name__ == '__main__':
    # Specify the path to the JPEG image
    image_path = sys.argv[1]

    # Call the image_publisher function with the specified topic and rate
    image_publisher(image_path, '/mgt/img_sampler/dock_cam/image_record/compressed', 5)
