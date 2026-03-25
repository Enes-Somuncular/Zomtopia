#!/bin/bash
cd "/Users/enessomuncular/Antigravity/Zomtopia"
echo "--- Zomtopia Derleniyor & Başlatılıyor ---"
rm -rf out && mkdir out
javac -d out $(find src -name "*.java")
java -cp out com.zomtopia.main.GameApp
