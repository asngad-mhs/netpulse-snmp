import sys
import os

try:
    from pptx import Presentation
    from pptx.util import Inches, Pt
    from pptx.dml.color import RGBColor
    from pptx.enum.text import PP_ALIGN
    from pptx.enum.shapes import MSO_SHAPE
except ImportError:
    print("python-pptx not available yet")
    sys.exit(1)

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5) # 16:9 Widescreen

# Color Palette
BG_COLOR = RGBColor(15, 23, 42)       # Dark Slate #0F172A
CARD_BG = RGBColor(30, 41, 59)        # Slate #1E293B
CYAN_ACCENT = RGBColor(0, 229, 255)   # Neon Cyan #00E5FF
EMERALD = RGBColor(16, 185, 129)      # Emerald Green #10B981
WHITE = RGBColor(248, 250, 252)       # White #F8FAFC
MUTED = RGBColor(148, 163, 184)       # Muted Slate #94A3B8
AMBER = RGBColor(245, 158, 11)        # Warning Amber #F59E0B

blank_slide_layout = prs.slide_layouts[6]

def add_base_slide(title_text, subtitle_text=""):
    slide = prs.slides.add_slide(blank_slide_layout)
    # Background fill
    bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg.fill.solid()
    bg.fill.fore_color.rgb = BG_COLOR
    bg.line.fill.background()

    # Top Header Banner
    header = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(0.5), Inches(11.733), Inches(1.1))
    header.fill.solid()
    header.fill.fore_color.rgb = CARD_BG
    header.line.color.rgb = CYAN_ACCENT
    header.line.width = Pt(1.5)

    tf = header.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = title_text
    p.font.size = Pt(24)
    p.font.bold = True
    p.font.color.rgb = CYAN_ACCENT

    if subtitle_text:
        p2 = tf.add_paragraph()
        p2.text = subtitle_text
        p2.font.size = Pt(12)
        p2.font.color.rgb = MUTED

    # Footer
    footer_box = slide.shapes.add_textbox(Inches(0.8), Inches(7.0), Inches(11.733), Inches(0.4))
    ftf = footer_box.text_frame
    fp = ftf.paragraphs[0]
    fp.text = "NetPulse SNMP • Sistem Monitoring Jaringan Mobile NOC • UNUGHA"
    fp.font.size = Pt(10)
    fp.font.color.rgb = MUTED

    return slide

def add_card(slide, left, top, width, height, title, items, border_color=CYAN_ACCENT):
    card = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(left), Inches(top), Inches(width), Inches(height))
    card.fill.solid()
    card.fill.fore_color.rgb = CARD_BG
    card.line.color.rgb = border_color
    card.line.width = Pt(1.2)

    tf = card.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = title
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = border_color
    p.space_after = Pt(8)

    for item in items:
        p_item = tf.add_paragraph()
        p_item.text = f"•  {item}"
        p_item.font.size = Pt(12)
        p_item.font.color.rgb = WHITE
        p_item.space_after = Pt(5)

# SLIDE 1: COVER
slide1 = prs.slides.add_slide(blank_slide_layout)
bg1 = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
bg1.fill.solid()
bg1.fill.fore_color.rgb = BG_COLOR
bg1.line.fill.background()

center_card = slide1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(1.8), Inches(1.2), Inches(9.733), Inches(5.1))
center_card.fill.solid()
center_card.fill.fore_color.rgb = CARD_BG
center_card.line.color.rgb = CYAN_ACCENT
center_card.line.width = Pt(2.5)

tf1 = center_card.text_frame
tf1.word_wrap = True

p_badge = tf1.paragraphs[0]
p_badge.alignment = PP_ALIGN.CENTER
p_badge.text = "NETWORK OPERATIONS CENTER (NOC) ENTERPRISE"
p_badge.font.size = Pt(13)
p_badge.font.bold = True
p_badge.font.color.rgb = EMERALD
p_badge.space_after = Pt(12)

p_title = tf1.add_paragraph()
p_title.alignment = PP_ALIGN.CENTER
p_title.text = "NetPulse SNMP"
p_title.font.size = Pt(44)
p_title.font.bold = True
p_title.font.color.rgb = CYAN_ACCENT

p_sub = tf1.add_paragraph()
p_sub.alignment = PP_ALIGN.CENTER
p_sub.text = "Aplikasi Mobile Monitoring Jaringan Real-Time Multi-Vendor Berbasis Android & SNMP"
p_sub.font.size = Pt(16)
p_sub.font.color.rgb = WHITE
p_sub.space_after = Pt(20)

p_meta = tf1.add_paragraph()
p_meta.alignment = PP_ALIGN.CENTER
p_meta.text = "Didukung: MikroTik RouterOS • Cisco Catalyst • Ruijie Reyee • OpenWrt Linux\nTerintegrasi Bot Peringatan Dini Telegram & OID Diagnostic Engine"
p_meta.font.size = Pt(12)
p_meta.font.color.rgb = MUTED

# SLIDE 2: LATAR BELAKANG
slide2 = add_base_slide("1. Latar Belakang & Permasalahan", "Tantangan Administrator Jaringan di Lingkungan Kampus / Instansi")
add_card(slide2, 0.8, 1.8, 5.7, 4.9, "Tantangan Lapangan", [
    "Ketergantungan pada PC/Laptop: Admin NOC harus membuka WinBox atau WebGUI di desktop untuk mengecek status router.",
    "Respon Gangguan yang Lambat: Ketika internet down di gedung tertentu, admin sering terlambat mengetahui masalah.",
    "Heterogenitas Perangkat: Kampus menggunakan router dan switch dari berbagai vendor (MikroTik, Cisco, Ruijie, OpenWrt) dengan tools terpisah.",
    "Beban Server NMS Berat: Aplikasi monitoring desktop/server seperti PRTG, Cacti, atau Zabbix memerlukan resource server tersendiri."
], AMBER)

add_card(slide2, 6.8, 1.8, 5.7, 4.9, "Kebutuhan Solusi Baru", [
    "Monitoring Portabel: Kemampuan memantau status jaringan langsung dari HP saat mobilitas di area kampus.",
    "Protokol Terstandar: Memanfaatkan protokol standar dunia SNMP (Simple Network Management Protocol) tanpa perlu install agent di router.",
    "Notifikasi Instan: Pengiriman alert otomatis ke grup Telegram tim teknis ketika trafik meledak atau router offline.",
    "Antarmuka Modern: UI responsif di HP (Galaxy A10) hingga Tablet (Galaxy Tab A7 Lite)."
], EMERALD)

# SLIDE 3: SOLUSI NETPULSE SNMP
slide3 = add_base_slide("2. Solusi Yang Dihadirkan: NetPulse SNMP", "Arsitektur Ringan Tanpa Server Perantara (Direct UDP Protocol)")
add_card(slide3, 0.8, 1.8, 3.7, 4.9, "Direct SNMP Engine", [
    "Berkomunikasi langsung via UDP Port 161 ke router.",
    "Membaca ASN.1 / BER encoding secara native tanpa pihak ketiga.",
    "Interval polling cepat (3 detik) untuk telemetri real-time.",
    "Bekerja di jaringan lokal Wi-Fi kampus maupun VPN."
], CYAN_ACCENT)

add_card(slide3, 4.8, 1.8, 3.7, 4.9, "Multi-Vendor Universal", [
    "MikroTik RouterOS (CCR, RB, Hex, CHR).",
    "Cisco IOS (Catalyst Switch, ISR Router).",
    "Ruijie Reyee (Smart Switch & AP Cloud).",
    "OpenWrt Linux (Gateway x86 & Embedded).",
    "Generic RFC 1213 MIB-II untuk perangkat lainnya."
], EMERALD)

add_card(slide3, 8.8, 1.8, 3.7, 4.9, "Early Warning Telegram", [
    "Integrasi resmi Telegram Bot API.",
    "Alert otomatis saat CPU Load melebihi batas (misal > 85%).",
    "Alert saat kabel putus atau router mati (SNMP Timeout).",
    "Format pesan rapi dengan ikon severity (CRITICAL/WARNING)."
], AMBER)

# SLIDE 4: ARSITEKTUR SISTEM
slide4 = add_base_slide("3. Arsitektur Sistem & Alur Kerja", "Diagram Aliran Data dari Router Hardware ke Layar Pengguna")
add_card(slide4, 0.8, 1.8, 5.7, 4.9, "Aliran Kerja Polling Telemetri", [
    "1. Inisialisasi: Aplikasi membaca konfigurasi node dari database lokal SQLite/Room.",
    "2. SNMP Request: Mengirim paket GetRequest PDU UDP Port 161 ke IP router (misal MikroTik kampus).",
    "3. Ekstraksi Data: Router membalas nilai OID spesifik (CPU, Oktet In/Out, Uptime, Klien).",
    "4. Kalkulasi Bandwidth: Menghitung selisih oktet byte per detik menjadi Kbps/Mbps.",
    "5. Rendering Visual: Menampilkan jarum gauge CPU dan grafik kanvas grafik fluida.",
    "6. Alert Evaluation: Memeriksa apakah metrik melanggar ambang batas telegram."
], CYAN_ACCENT)

add_card(slide4, 6.8, 1.8, 5.7, 4.9, "Keunggulan Arsitektur Ini", [
    "Zero-Server Requirement: Tidak memerlukan server backend perantara; HP bertindak langsung sebagai NMS client.",
    "Data 100% Privat: Kredensial router dan community string hanya tersimpan aman di database lokal HP.",
    "Offline-Ready: Tetap dapat membuka riwayat perangkat dan data log alert meskipun jaringan sedang terputus.",
    "Konsumsi Baterai Rendah: Menggunakan Coroutines Flow dengan pembacaan asinkron tanpa membebani CPU smartphone."
], EMERALD)

# SLIDE 5: FITUR-FITUR UTAMA
slide5 = add_base_slide("4. Fitur-Fitur Utama Aplikasi", "Enam Modul Fungsional Lengkap untuk NOC Administrator")
add_card(slide5, 0.8, 1.8, 3.7, 2.3, "1. Portal Login Keamanan", [
    "Autentikasi operator NOC sebelum masuk dashboard.",
    "Tersedia fitur 'Ingat Sesi Masuk' & ganti kata sandi.",
    "Kredensial bawaan: admin / admin123."
], CYAN_ACCENT)

add_card(slide5, 4.8, 1.8, 3.7, 2.3, "2. Dashboard Telemetri", [
    "CPU Load Gauge analog 0-100% dinamis.",
    "Grafik kanvas real-time Download/Upload.",
    "Uptime jam:menit dan jumlah klien aktif."
], EMERALD)

add_card(slide5, 8.8, 1.8, 3.7, 2.3, "3. Manajemen Interface", [
    "Daftar port fisik: ether, sfp, vlan, bridge.",
    "Status link UP / DOWN warna hijau/merah.",
    "Statistik akumulasi byte RX dan TX."
], AMBER)

add_card(slide5, 0.8, 4.4, 3.7, 2.3, "4. VLAN & DHCP Leases", [
    "Inspeksi ID VLAN dan subnet alokasi.",
    "Daftar perangkat klien (IP, MAC, Hostname).",
    "Status sewa DHCP: Bound atau Static."
], AMBER)

add_card(slide5, 4.8, 4.4, 3.7, 2.3, "5. Telegram Notifier", [
    "Bot token dan Chat ID testing bawaan.",
    "Slider konfigurasi ambang batas CPU.",
    "Riwayat log peringatan lengkap waktu kejadian."
], CYAN_ACCENT)

add_card(slide5, 8.8, 4.4, 3.7, 2.3, "6. SNMP OID Diagnostic", [
    "Peralatan MIB Walker mandiri di aplikasi.",
    "Dapat memasukkan custom OID apa saja.",
    "Ukur latensi respon ping paket SNMP dalam ms."
], EMERALD)

# SLIDE 6: VALIDITAS DATA MIKROTIK
slide6 = add_base_slide("5. Pembuktian Validitas Data Riil MikroTik", "Daftar OID Resmi MIB yang Dibaca dari RouterOS Kampus")
add_card(slide6, 0.8, 1.8, 5.7, 4.9, "Pemetaan OID Resmi di Aplikasi", [
    "CPU Load: .1.3.6.1.4.1.14988.1.1.1.3.1.5.1 (mtxrHlProcessorUsage) & fallback .1.3.6.1.2.1.25.3.3.1.2.1 (hrProcessorLoad).",
    "Download Octets: .1.3.6.1.2.1.31.1.1.1.6.1 (ifHCInOctets 64-bit High Capacity) untuk akurasi trafik gigabit.",
    "Upload Octets: .1.3.6.1.2.1.31.1.1.1.10.1 (ifHCOutOctets 64-bit High Capacity).",
    "System Uptime: .1.3.6.1.2.1.1.3.0 (sysUpTimeInstance) dalam satuan centiseconds.",
    "Client Count: .1.3.6.1.4.1.14988.1.1.1.2.1.1 (Wireless reg table / ARP count)."
], CYAN_ACCENT)

add_card(slide6, 6.8, 1.8, 5.7, 4.9, "Dua Mode Operasional Fleksibel", [
    "1. Mode Perangkat Riil (Mode Simulasi = OFF):",
    "   • Mengirim request paket SNMP sebenarnya ke IP MikroTik kampus.",
    "   • Jika router mati, langsung mendeteksi SNMP Timeout dan mengirim alert Telegram.",
    "   • Nilai trafik, CPU, dan status 100% valid dari hardware fisik.",
    "2. Mode Simulasi Edukasi (Mode Simulasi = ON):",
    "   • Sangat berguna untuk presentasi, demo kelas, atau saat tidak berada di area kampus.",
    "   • Menghasilkan simulasi fluktuasi trafik realistis untuk menguji respon grafik."
], EMERALD)

# SLIDE 7: KEAMANAN & MANAJEMEN AKUN
slide7 = add_base_slide("6. Keamanan & Sistem Login Admin", "Mencegah Akses Tidak Sah ke Konsol Pengawasan Jaringan")
add_card(slide7, 0.8, 1.8, 5.7, 4.9, "Fitur Keamanan Aplikasi", [
    "Portal Login Mandiri: Layar utama terkunci sebelum operator memasukkan username & password yang tepat.",
    "Toggle Visibility Password: Fitur intip kata sandi untuk mencegah salah ketik.",
    "Penyimpanan Sesi Aman: Menggunakan enkripsi preferensi lokal Android, data sesi aman dari pembajakan aplikasi lain.",
    "Tombol Logout Terproteksi: Dilengkapi konfirmasi dialog di menu atas dan menu pengaturan.",
    "Fitur Ganti Sandi: Admin dapat memperbarui kata sandi default 'admin123' kapan saja di menu pengaturan."
], CYAN_ACCENT)

add_card(slide7, 6.8, 1.8, 5.7, 4.9, "Praktik Keamanan SNMP Lapangan", [
    "SNMP Community String: Aplikasi mendukung penggantian 'public' ke string rahasia organisasi.",
    "Pembatasan Subnet IP: Di sisi MikroTik, menu '/snmp community' dapat disetting hanya merespon IP subnet smartphone teknisi.",
    "SNMP Port Custom: Port UDP dapat dipindahkan dari 161 ke port khusus untuk menghindari port scanner.",
    "No-Cloud Leakage: Tidak ada data telemetri yang dikirim ke server pihak ketiga (kecuali bot Telegram yang disetujui pengguna)."
], EMERALD)

# SLIDE 8: TEKNOLOGI YANG DIGUNAKAN
slide8 = add_base_slide("7. Teknologi & Stack Pengembangan", "Dibangun dengan Standar Arsitektur Modern Android Google")
add_card(slide8, 0.8, 1.8, 5.7, 4.9, "Android Modern Tech Stack", [
    "Bahasa Pemrograman: 100% Kotlin dengan Coroutines & StateFlow asinkron.",
    "UI Framework: Jetpack Compose + Material Design 3 (M3).",
    "Database Lokal: Android Room Database (SQLite Engine) untuk persistensi node dan alert.",
    "Pola Desain: MVVM (Model-View-ViewModel) + Clean Repository Pattern.",
    "Visualisasi: Custom Canvas drawing untuk grafik bandwidth dinamis dan gauge jarum CPU.",
    "Responsif: Dukungan tata letak adaptif untuk smartphone (Galaxy A10) dan tablet (Galaxy Tab A7 Lite)."
], CYAN_ACCENT)

add_card(slide8, 6.8, 1.8, 5.7, 4.9, "Spesifikasi Minimal Perangkat", [
    "Sistem Operasi: Android 8.0 (Oreo) hingga Android 14+.",
    "RAM Minimum: 1 GB (Sangat ringan dan hemat daya baterai).",
    "Izin Sistem: Menggunakan izin INTERNET murni tanpa meminta izin berbahaya (storage/lokasi).",
    "Kompatibilitas Layar: Compact Phone (Handheld) dan Expanded Screen (Tablet Navigation Rail).",
    "Ukuran APK: Hanya ~15 MB, cepat diinstal di berbagai tipe smartphone teknisi."
], EMERALD)

# SLIDE 9: KESIMPULAN
slide9 = add_base_slide("8. Kesimpulan & Roadmap Pengembangan", "Ringkasan Nilai Manfaat dan Rencana Pengembangan ke Depan")
add_card(slide9, 0.8, 1.8, 5.7, 4.9, "Nilai Manfaat Utama", [
    "1. Efisiensi Waktu: Teknisi dapat mendiagnosis kesehatan jaringan saat berada di lapangan tanpa harus kembali ke ruang NOC.",
    "2. Multi-Vendor Agnostik: Satu aplikasi dapat memantau MikroTik, Cisco, Ruijie, dan OpenWrt secara bersamaan.",
    "3. Peringatan Dini: Bot Telegram memastikan insiden jaringan dapat ditangani sebelum dikeluhkan pengguna.",
    "4. Bebas Biaya Server: Tidak butuh sewa VPS atau server NMS khusus."
], EMERALD)

add_card(slide9, 6.8, 1.8, 5.7, 4.9, "Rencana Pengembangan (Roadmap)", [
    "• SNMP v3 Support: Penambahan enkripsi AuthPriv (USM, SHA-256, AES-128).",
    "• Auto Network Discovery: Fitur pemindaian otomatis seluruh perangkat dalam subnet IP /24.",
    "• Ekspor Laporan: Pembuatan laporan performa bulanan berformat PDF/Excel.",
    "• Widget Layar Utama: Menampilkan ringkasan status router di home screen Android."
], CYAN_ACCENT)

# SLIDE 10: PENUTUP & TANYA JAWAB
slide10 = prs.slides.add_slide(blank_slide_layout)
bg10 = slide10.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
bg10.fill.solid()
bg10.fill.fore_color.rgb = BG_COLOR
bg10.line.fill.background()

end_card = slide10.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(2.2), Inches(1.5), Inches(8.933), Inches(4.5))
end_card.fill.solid()
end_card.fill.fore_color.rgb = CARD_BG
end_card.line.color.rgb = CYAN_ACCENT
end_card.line.width = Pt(2.0)

etf = end_card.text_frame
etf.word_wrap = True

ep1 = etf.paragraphs[0]
ep1.alignment = PP_ALIGN.CENTER
ep1.text = "TERIMA KASIH"
ep1.font.size = Pt(36)
ep1.font.bold = True
ep1.font.color.rgb = CYAN_ACCENT
ep1.space_after = Pt(14)

ep2 = etf.add_paragraph()
ep2.alignment = PP_ALIGN.CENTER
ep2.text = "NetPulse SNMP - Enterprise Network Monitoring in Your Pocket"
ep2.font.size = Pt(16)
ep2.font.bold = True
ep2.font.color.rgb = WHITE
ep2.space_after = Pt(20)

ep3 = etf.add_paragraph()
ep3.alignment = PP_ALIGN.CENTER
ep3.text = "Sesi Diskusi & Tanya Jawab (Q&A)\n\nUniversitas Nahdlatul Ulama Al Ghazali (UNUGHA)"
ep3.font.size = Pt(14)
ep3.font.color.rgb = MUTED

output_file = "NetPulse-SNMP-Presentasi.pptx"
prs.save(output_file)
print(f"SUCCESS: Presentation saved to {output_file}")
