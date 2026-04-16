# 🧟 Zomtopia

> [!CAUTION]
> **BU PROJE ARŞİVLENMİŞTİR.**
> Bu uygulamanın geliştirilmesi durdurulmuştur ve artık aktif olarak desteklenmemektedir.


Zomtopia, Java Swing kullanılarak geliştirilmiş, 2D piksel sanat tarzında bir hayatta kalma ve inşa etme (sandbox) oyunudur. 

> [!IMPORTANT]
> **Bu proje tamamen Yapay Zeka (Antigravity) tarafından tasarlanmış ve kodlanmıştır.** 
> Kod yapısından oyun mekaniklerine, dünya nesillerinden envanter sistemine kadar her detay AI tarafından oluşturulmuştur.

## 🌟 Öne Çıkan Özellikler

- **Prosedürel Dünya Nesli:** Her yeni oyun için rastgele oluşturulan 100x100 boyutunda, katmanlı bir dünya.
- **Dinamik Gündüz/Gece Döngüsü:** Gökyüzü renklerinin değiştiği ve zamanın aktığı gerçekçi bir atmosfer.
- **Hayatta Kalma Mekanikleri:** Can (Health), Açlık (Hunger) ve Kondisyon (Stamina) sistemleri.
- **Gelişmiş Envanter ve Üretim:** 
  - Karakter üzerinde 2x2 üretim.
  - Çalışma Masası (Crafting Table) ile 3x3 genişletilmiş üretim.
  - Sürükle-bırak destekli envanter yönetimi ve otomatik sıralama.
- **İnşa ve Madencilik:** Blok kırma (Mining) ve yerleştirme (Building) mekanikleri.
- **Ekipman Sistemi:** Farklı kategorilerde (Kask, Maske, Üst, Alt, Ayakkabı, Sırt/Kanat) giyilebilir eşyalar.
- **Karakter Özelleştirme:** Giyilen eşyaların anlık olarak karakter üzerinde görünmesi.
- **Kayıt Sistemi:** Oyun ilerlemesini `savegame.dat` dosyasına kaydedip geri yükleme imkanı.

## 🎮 Kontroller

| Tuş | İşlem |
| :--- | :--- |
| **W, A, S, D** veya **Ok Tuşları** | Hareket Etme ve Zıplama |
| **Sol Shift** | Koşma (Stamina harcar) |
| **Sol Tık (Basılı Tut)** | Blok Kırma / Madencilik |
| **Sağ Tık** | Blok Yerleştirme / Eşya Kuşanma |
| **Fare Tekerleği** veya **1-9** | Hızlı Erişim Çubuğu (Hotbar) Seçimi |
| **E** | Envanteri Aç/Kapat |
| **Z** | Envanteri Otomatik Sırala |
| **ESC** | Oyun Menüsü / Duraklatma |

## 🛠️ Teknik Detaylar

- **Dil:** Java
- **Kütüphane:** Java Swing & AWT (Harici bir oyun motoru kullanılmamıştır)
- **Mimari:** OOP (Nesne Yönelimli Programlama), MVC benzeri yapı.
- **Çözünürlük:** 800x450 (16:9 Mobil/Yatay oranına göre optimize edilmiştir)

## 🚀 Nasıl Çalıştırılır?

1. Bilgisayarınızda **Java JDK** (v8 veya üstü) yüklü olduğundan emin olun.
2. Proje dizinindeki `Zomtopia.bat` (Windows için) veya `Zomtopia.command` (macOS/Linux için) dosyasını çalıştırın.
3. Manuel çalıştırmak isterseniz:
   ```bash
   javac -d out @sources.txt
   java -cp out com.zomtopia.main.GameApp
   ```

---
*Bu proje, AI'nın yazılım geliştirme yeteneklerini sergilemek amacıyla **Antigravity** tarafından oluşturulmuştur.*
