A calorie tracking app utilizing computer vision offers a cutting-edge solution for managing dietary intake. By employing advanced image recognition technology, the app can accurately identify fruit/image items and estimate their calorie content from photos taken by the user. This innovative approach streamlines the process of logging meals, making it quicker and more user-friendly. Users simply snap a picture of their fruit/vegetable, and the app processes the image to provide detailed nutritional information. This not only enhances convenience but also improves accuracy in calorie tracking, helping users make informed dietary choices and achieve their health and fitness goals more effectively.

The app has a camera UI with a camera button to click pictures, a gallery button to retrieve images from gallery and a search button to retrieve images clicked inside the app from a database and a reset button to reset camera position after clicking the camera button. So what the app does is, when a user takes a photo or uploads images from either gallery or the app's database, the tensorflow lite model predicts the fruit/vegetable in the image along with its nutritional info stored in a json file and displays the results on a pop-up window along with a recommendation system which tells the user top 3 fruits/vegetables similar to the one predicted by the model in the pop-up window.


Features:

1) A clean UI and perfect image buttons for reset, camera, gallery and retrieval.
2) Database integration- if you click on the search button a new screen pops up where all the photos clicked in the app gets stored on the screen, Also stores photos
   uploaded from the gallery.
3) The users have option to retrieve and delete images from the database.
4) Tensorflowlite model integration.
