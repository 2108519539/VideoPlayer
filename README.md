VideoPlayer2

---

### 全屏切换
1. 基本技术
  + 子视图在父视图中切换
2. [android 全屏切换：去掉状态栏虚拟按键等](https://blog.csdn.net/qq_32671919/article/details/107035353)
3. [Activity去掉状态栏](https://blog.csdn.net/lincyang/article/details/42673151)

### 屏幕旋转
1. [android:configChanges="orientation|keyboardHidden|screenSize"](https://blog.csdn.net/lkk790470143/article/details/79345971)
2. [代码中动态切换横竖屏](https://www.jianshu.com/p/dbc7e81aead2)
~~~java
      //判断当前屏幕方向  
      if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {     
       //切换竖屏   
         MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);   
      }else{   
         //切换横屏     
         MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);  
      } 
~~~

### FrameLayout 点击穿透问题
1. [Android 点击事件穿透处理](https://www.it610.com/article/1297748543011889152.htm)
2. [Android防止点击事件穿透](https://blog.csdn.net/hello_worldhzx/article/details/96132803)