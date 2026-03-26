#!/bin/bash
cd "$(dirname "$0")"
echo "--- Zomtopia Derleniyor & Başlatılıyor ---"
rm -rf out && mkdir out
javac -d out $(find src -name "*.java")
java -cp out com.zomtopia.main.GameApp
