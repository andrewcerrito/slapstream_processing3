class Obstacle {

  PImage[] asteroids;
  PImage currentGraphic;
  int maxSize = 150; // dictates maximum size of obstacle
  int x; 
  float y;
  int w = (int) random(30, maxSize);
  float rad;
  float obstSpeed = 0;
  float speedModifier = random(-2, 2); // makes the obstacles fall at slightly different speed, more staggered
  boolean graphicSelected = false;

  Obstacle (int tx, float ty) {
    x = tx;
    y = ty;
    rad = w/2;
  }

  void imageInit() {
    asteroids = new PImage[7];
    for (int i = 1; i < 8; i++) {
      println("image " + i + " loaded.");
      // Use nf() to number format 'i' into four digits
      String filename = "asteroid_" + nf(i, 4) + ".png";
      println(filename);
      asteroids[i-1] = loadImage(filename);
    }
  }

  void imageSelect() {
    if (graphicSelected == false) {
      int imageSelector = (int)random(1, 8);
      switch(imageSelector) {
      case 1:
        //    image(asteroids[0], x, y);
        asteroids[0].resize(int(w*1.4), int(w*1.4));
        currentGraphic = asteroids[0];
        break;
      case 2:
        //      image(asteroids[1], x, y);
        asteroids[1].resize(int(w*1.3), int(w*1.3));
        currentGraphic = asteroids[1];
        break;
      case 3:
        //      image(asteroids[2], x, y);
        asteroids[2].resize(int(w*1.1), int(w*1.1));
        currentGraphic = asteroids[2];
        break;
      case 4:
        //      image(asteroids[3], x, y);
        asteroids[3].resize(int(w*1.1), int(w*1.1));
        currentGraphic = asteroids[3];
        break;
      case 5:
        //      image(asteroids[4], x, y);
        asteroids[4].resize(int(w*1.2), int(w*1.2));
        currentGraphic = asteroids[4];
        break;
      case 6:
        //      image(asteroids[5], x, y);
        asteroids[5].resize(int(w*1.1), int(w*1.1));
        currentGraphic = asteroids[5];
        break;
      case 7:
        //      image(asteroids[6], x, y);
        asteroids[6].resize(int(w*1.1), int(w*1.1));
        currentGraphic = asteroids[6];
        break;
      }
    }
    graphicSelected = true;
  }


  void display() {
    pushStyle();
    noFill();
    noStroke();
    //    fill(255, 0, 0, 100);
    //    stroke(255);
    imageMode(CENTER);
    image(currentGraphic, x, y);
    ellipse(x, y, w, w);
    popStyle();
  }

  void move() {
    obstSpeed = (float) (millis()-millisSinceGameEnd)/9000; // make speed increase the longer the game goes on
    y= y + 4 + obstSpeed + speedModifier; // move down the screen
    if (y >= height + rad) { // if circle leaves bottom of screen:
      y = (int) -rad; // reset to top of screen
      x = (int) random(0, 600); // get a new random x-position - SET TO 600 FOR NOW, CHANGE BACK LATER
      w = (int) random(30, maxSize); // get a new random size
      graphicSelected = false; // get a new asteroid graphic
      imageSelect();
      rad = w/2; // correct the radius variable with the new width
    }
  }
  
}
