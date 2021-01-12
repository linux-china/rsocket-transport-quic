# install native library for Mac
mac-install:
  wget https://repo1.maven.org/maven2/io/netty/incubator/netty-incubator-codec-quic/0.0.3.Final/netty-incubator-codec-quic-0.0.3.Final-osx-x86_64.jar
  unzip -d temp netty-incubator-codec-quic-0.0.3.Final-osx-x86_64.jar
  cp temp/META-INF/native/libnetty_quiche_osx_x86_64.jnilib  ~/Library/Java/Extensions/
  rm -rf temp
  rm -rf netty-incubator-codec-quic-0.0.3.Final-osx-x86_64.jar