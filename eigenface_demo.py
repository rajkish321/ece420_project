#!/usr/bin/env python
# coding: utf-8

# In[1]:


import cv2
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import os
from PIL import Image
import scipy.spatial


# In[2]:


images = {} #this will store images of each person
#loading all images into the "images" dictionary
for image_str in os.listdir('pics'):
    name_arr = image_str.split('-')
    name = name_arr[0] +' ' + name_arr[1]
    if images.get(name,0) == 0:
        images[name] = []
    img = Image.open('pics/'+image_str)
    new_img = img.resize((300,400))    #resize all pics to same size
    images[name].append(new_img.convert('L'))

num_faces = len(images.keys())*4


# In[3]:


avg_face = np.zeros((400,300))


for key in images.keys():
    for i in range(4):
        avg_face += np.array(images[key][i].convert('L'))


avg_face /= num_faces
plt.imshow(avg_face)
plt.show()
avg_face.shape


# In[4]:


A = np.empty((len(images.keys())*4,400*300))

face_idx = 0
for key in images.keys():
    for i in range(4):
        orig_face = images[key][i].convert('L')
        A[face_idx] = (np.array(orig_face)-avg_face).flatten()
        face_idx+=1


# In[5]:


A = A.T


# In[6]:


L = np.matmul(A.T,A)
L /= num_faces


# In[7]:


evals, evects = np.linalg.eig(L)

sort_indices = evals.argsort()[::-1]
evals = evals[sort_indices]
evects = evects[:,sort_indices]

M_prime = (int(num_faces/3))
evals = evals[:M_prime]
evects = evects[:,:M_prime]


# In[8]:


evects = np.dot(A,evects)


# In[9]:


evects = evects/np.linalg.norm(evects)


# In[10]:


W_k = np.dot(evects.T,A)


# In[11]:


#now onto testing:


# In[14]:


new_faces = {}
for image_str in os.listdir('test'):
    name_arr = image_str.split('-')
    name = name_arr[0] +' ' + name_arr[1]
    img = Image.open('test/'+image_str)
    img = img.resize((300,400)).convert('L')
    new_faces[name] = img.convert('L')

    plt.imshow(img)
    plt.show()


# In[27]:


img = new_faces['red apple']

# img = images['will smith'][1]
for key in new_faces.keys():
    img = new_faces[key]
    img = (img - avg_face).reshape(120000,1)

    W = np.dot(evects.T,img)
    norms = np.linalg.norm(W-W_k, axis = 0)

    avg_norms = np.zeros(int(num_faces/4))
    for i in range(len(avg_norms)):
        for j in range(4):
            avg_norms[i] += norms[i*4+j]
    avg_norms/=4
#     print(avg_norms) #check for recognition

    phi_hat = np.zeros(120000)
    phi = img.reshape(120000)
    for i in range(M_prime):
        phi_hat += evects.T[i]*phi

    norms_phi = np.linalg.norm(phi-phi_hat.reshape(120000), axis = 0)

    norms_phi #check for detection

    idx = np.argmin(avg_norms)
    detection_threshold = 24000
    recognition_threshold = 7000
    if norms_phi <detection_threshold:
        if avg_norms[idx]<recognition_threshold:
            print("We classify this as", list(images)[idx])
        else:
            print("We classify this as an unknown person")
    else:
        print("We classify this as not a face")

    plt.imshow(img.reshape((400,300)))
    plt.show()



# In[28]:


for key in images.keys():
    for index in range(4):
        img = np.array(images[key][index])
        img = (img - avg_face).reshape(120000,1)

        W = np.dot(evects.T,img)
        norms = np.linalg.norm(W-W_k, axis = 0)

        avg_norms = np.zeros(int(num_faces/4))
        for i in range(len(avg_norms)):
            for j in range(4):
                avg_norms[i] += norms[i*4+j]
        avg_norms/=4
    #     print(avg_norms) #check for recognition

        phi_hat = np.zeros(120000)
        phi = img.reshape(120000)
        for i in range(M_prime):
            phi_hat += evects.T[i]*phi

        norms_phi = np.linalg.norm(phi-phi_hat.reshape(120000), axis = 0)

        norms_phi #check for detection

        idx = np.argmin(avg_norms)
        detection_threshold = 24000
        recognition_threshold = 7000
        if norms_phi <detection_threshold:
            if avg_norms[idx]<recognition_threshold:
                print("We classify this as", list(images)[idx])
            else:
                print("We classify this as an unknown person")
        else:
            print("We classify this as not a face")

        plt.imshow(img.reshape((400,300)))
        plt.show()


# In[ ]:


plt.imshow(evects.T[0].reshape((400,300)))


# In[ ]:





# In[ ]:





# In[ ]:


projection = W.T*evects


# In[ ]:


for i in range(num_faces):
    plt.imshow(projection.T[i].reshape(400,300),cmap = 'gray')
    plt.show()
