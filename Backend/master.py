import os
import cv2
import requests
from flask import Flask, request
from mnistModel import detect_num as detect
import numpy as np

app = Flask(__name__)

absolutePath = os.path.dirname(__file__)
relativePath = os.path.join(absolutePath, 'categories/')

@app.route('/', methods=['POST'])
def upload():

    file1 = request.files['image']
    img = cv2.imdecode(np.fromstring(request.files['image'].read(), np.uint8), cv2.IMREAD_UNCHANGED)
    # Get image dimensions 
    (h, w) = img.shape[:2]
    # cv2.imshow('Original', img)
    
    # compute the center coordinate of the image
    (cX, cY) = (w // 2, h // 2)

    # crop the image into four parts which will be labelled as
    # top left, top right, bottom left, and bottom right.
    topLeft = img[0:cY, 0:cX]
    topRight = img[0:cY, cX:w]
    bottomLeft = img[cY:h, 0:cX]
    bottomRight = img[cY:h, cX:w]
    quad=[topLeft,topRight,bottomLeft,bottomRight]
    part = int(request.form['quad'])
    # visualize the cropped regions
    # cv2.imwrite("Top Left Corner.jpg", topLeft)
    # cv2.imwrite("Top Right Corner.jpg", topRight)
    # cv2.imwrite("Bottom Right Corner.jpg", bottomLeft)
    # cv2.imwrite("Bottom Left Corner.jpg", bottomRight)
    # cv2.waitKey(0)

    if part==0:
        num=detect(topLeft)
        # cv2.imwrite("Top Left Corner.jpg", topLeft)
    elif part==1:
        num=detect(topRight)
        # cv2.imwrite("Top Right Corner.jpg", topRight)
    elif part==2:
        num=detect(bottomLeft)
        # cv2.imwrite("Bottom Right Corner.jpg", bottomLeft)
    elif part==3:
        num=detect(bottomRight)
        # cv2.imwrite("Bottom Left Corner.jpg", bottomRight)
    else:
        num = detect(topLeft)

    return str(num)

    


if __name__ == '__main__':
    app.run(host='0.0.0.0')

