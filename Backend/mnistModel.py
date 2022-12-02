from keras.models import load_model
import cv2
import numpy as np
import matplotlib.pyplot as plt 
from PIL import Image, ImageOps


def preprocess_images(imgs): # should work for both a single image and multiple images
    sample_img = imgs if len(imgs.shape) == 2 else imgs[0]
    assert sample_img.shape in [(28, 28, 1), (28, 28)], sample_img.shape # make sure images are 28x28 and single-channel (grayscale)
    return imgs / 255.0



def detect_num(img):
    img_proc = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    (thresh, blackAndWhiteImage) = cv2.threshold(img_proc, 127, 255, cv2.THRESH_BINARY)
    img_proc = cv2.resize(blackAndWhiteImage, (28, 28))

    img_proc = preprocess_images(img_proc)

    img_proc = 1 - img_proc # inverse since training dataset is white text with black background


    net_in = np.expand_dims(img_proc, axis=0)
    y_pred = model.predict(net_in)[0]
    print(y_pred)
    print("Prediction :",np.argmax(y_pred))
    return str(np.max(y_pred))+","+str(np.argmax(y_pred))

    
model = load_model('mnist.h5')
