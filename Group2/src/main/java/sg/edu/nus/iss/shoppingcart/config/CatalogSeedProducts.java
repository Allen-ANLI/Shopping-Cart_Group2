package sg.edu.nus.iss.shoppingcart.config;

import sg.edu.nus.iss.shoppingcart.entity.Product;
import java.math.BigDecimal;
import java.util.List;

/** Local sample catalogue with fictional brands; customer reviews are never fabricated. */
final class CatalogSeedProducts {
    private CatalogSeedProducts() {}
    // category | name | Chinese name | price | image | brand | description | Chinese description
    private static final String ROWS = """
typing|Keyboard|日常办公键盘|50.00|keyboard.png|Keycraft|A comfortable full-size keyboard with quiet keys and a USB connection.|舒适的全尺寸键盘，配备安静按键和 USB 连接。
typing|Mouse|无线办公鼠标|20.00|mouse.png|Keycraft|A compact wireless mouse with adjustable sensitivity for work and study.|小巧的无线鼠标，灵敏度可调，适合办公与学习。
audio|Headphones|舒适头戴耳机|79.00|headphones.svg|Soundlane|Comfortable over-ear headphones with padded cushions for online classes and focused work.|柔软耳垫包覆双耳，适合网课和专注工作。
computing|USB-C Hub|多口 USB-C 扩展坞|39.90|usb-c-hub.svg|Portwell|A compact multi-port USB-C hub with USB-A, HDMI and card reader connections.|集 USB-A、HDMI 与读卡接口于一体的便携扩展坞。
workspace|Laptop Stand|铝合金笔记本支架|35.00|laptop-stand.svg|Deskfolk|An aluminium stand with an open frame to raise your laptop and improve airflow.|开放式铝合金支架抬高屏幕，兼顾舒适视角与散热。
workspace|Desk Lamp|可调节 LED 台灯|29.00|desk-lamp.svg|Deskfolk|A warm LED desk lamp with an adjustable arm for reading and evening study.|可调节灯臂的暖光 LED 台灯，适合阅读与夜间学习。
computing|Webcam|高清 USB 网络摄像头|59.90|webcam.svg|Portwell|A 1080p USB webcam with a monitor clip for video meetings and online lessons.|1080p USB 摄像头搭配显示器夹，适合视频会议与网课。
computing|Portable SSD|便携固态硬盘 500GB|99.90|portable-ssd.svg|Bytewood|A 500GB portable solid-state drive with USB-C for documents, photos and projects.|500GB USB-C 便携固态硬盘，方便存储文档、照片与项目。
computing|Monitor|24 英寸办公显示器|199.90|monitor.svg|Bytewood|A 24-inch full-HD monitor with HDMI input and an adjustable tilt stand.|24 英寸全高清显示器，配备 HDMI 接口与可调倾斜支架。
audio|Bluetooth Speaker|便携蓝牙音箱|49.90|bluetooth-speaker.svg|Soundlane|A compact rechargeable Bluetooth speaker for music and podcasts at your desk.|可充电蓝牙音箱，桌面听歌与播客更方便。
workspace|Desk Mat|柔软防滑桌垫|12.90|desk-mat.svg|Deskfolk|A 70cm soft desk mat with a non-slip base for your keyboard and mouse.|70 厘米柔软桌垫，防滑底面让键盘与鼠标保持稳定。
audio|USB Microphone|桌面 USB 麦克风|89.90|usb-microphone.svg|Soundlane|A USB condenser microphone with a desktop stand and mute control for clear calls.|USB 电容麦克风带桌面支架和静音控制，通话录音更清晰。
computing|Studio 27 Monitor|Studio 27 英寸显示器|289.00|monitor.svg|Bytewood|A 27-inch QHD display with a slim bezel and HDMI input for spreadsheets and creative work.|27 英寸 QHD 窄边框显示器，适合表格处理和创意工作。
computing|Pocket SSD 1TB|Pocket 1TB 移动固态硬盘|149.00|portable-ssd.svg|Bytewood|A 1TB USB-C solid-state drive in an aluminium enclosure for large project libraries.|铝合金外壳的 1TB USB-C 固态硬盘，轻松容纳大型项目。
computing|Travel Hub 4-in-1|旅行四合一扩展坞|24.90|usb-c-hub.svg|Portwell|A lightweight four-port USB hub for connecting a keyboard, mouse and storage on the go.|轻便四口 USB 扩展坞，出行时连接键鼠与存储设备。
computing|Creator Hub 8-in-1|创作八合一扩展坞|69.00|usb-c-hub.svg|Portwell|An eight-port USB-C dock with HDMI, Ethernet and SD reader for a complete workspace.|八口 USB-C 扩展坞，含 HDMI、网口与 SD 读卡器。
computing|Focus Webcam 2K|Focus 2K 网络摄像头|89.00|webcam.svg|Portwell|A 2K webcam with a privacy cover and adjustable monitor mount for home meetings.|2K 网络摄像头带隐私遮挡盖与可调支架，适合居家会议。
computing|Archive SSD 2TB|Archive 2TB 固态硬盘|239.00|portable-ssd.svg|Bytewood|A roomy 2TB external SSD with USB-C for video archives and regular backups.|2TB 大容量 USB-C 移动固态硬盘，适合视频归档与日常备份。
typing|Compact 65 Keyboard|Compact 65 配列键盘|69.00|keyboard.png|Keycraft|A space-saving 65-percent mechanical keyboard with arrow keys and a detachable USB-C cable.|65 配列机械键盘保留方向键，可拆卸 USB-C 线节省桌面空间。
typing|Studio TKL Keyboard|Studio 无数字区键盘|89.00|keyboard.png|Keycraft|A tenkeyless mechanical keyboard with tactile switches for comfortable long typing sessions.|无数字区机械键盘采用段落轴体，长时间输入也舒适。
typing|Quiet Wireless Keyboard|Quiet 无线静音键盘|59.00|keyboard.png|Keycraft|A slim wireless keyboard with low-profile quiet keys and a rechargeable battery.|轻薄无线键盘，低矮静音按键搭配可充电电池。
typing|Ergo Split Keyboard|Ergo 人体工学分体键盘|119.00|keyboard.png|Keycraft|A split-layout wired keyboard with a gentle wrist angle and dedicated shortcut keys.|分体布局有线键盘，温和手腕角度与专用快捷键提升舒适度。
typing|Precision Wireless Mouse|Precision 无线精准鼠标|45.00|mouse.png|Glideworks|A rechargeable wireless mouse with a precision wheel and three sensitivity presets.|可充电无线鼠标，精准滚轮与三档灵敏度切换。
typing|Travel Mini Mouse|Travel 迷你便携鼠标|18.90|mouse.png|Glideworks|A pocket-size wireless mouse with a stored USB receiver for working away from home.|口袋大小无线鼠标，可收纳 USB 接收器，便于移动办公。
typing|Ergo Vertical Mouse|Ergo 垂直人体工学鼠标|49.00|mouse.png|Glideworks|A vertical wireless mouse designed for a relaxed hand position with two thumb buttons.|垂直无线鼠标帮助保持放松手势，配备两个拇指按键。
typing|Performance Gaming Mouse|Performance 轻量游戏鼠标|59.90|mouse.png|Glideworks|A lightweight wired mouse with adjustable sensitivity and six programmable buttons.|轻量有线鼠标，灵敏度可调并配有六个可编程按键。
workspace|Foldaway Laptop Stand|Foldaway 折叠笔记本支架|24.90|laptop-stand.svg|Deskfolk|A foldable laptop riser with six height settings and a carrying sleeve for travel.|六档高度可调的折叠支架，附便携收纳袋。
workspace|Elevate Pro Stand|Elevate Pro 升降支架|59.00|laptop-stand.svg|Deskfolk|A sturdy aluminium laptop stand with adjustable height and a swivel base.|坚固铝合金笔记本支架，高度可调，底座支持旋转。
workspace|Reading Lamp Plus|Reading Plus 阅读台灯|45.00|desk-lamp.svg|LumaDesk|An LED reading lamp with three colour temperatures, dimming and a flexible arm.|LED 阅读台灯支持三种色温、亮度调节与灵活灯臂。
workspace|Portable Study Lamp|便携充电学习灯|22.90|desk-lamp.svg|LumaDesk|A rechargeable desk light with a fold-flat design for study rooms and travel.|可充电桌面灯支持折叠收纳，适合自习室和旅行。
workspace|Wide Felt Desk Mat|加宽毛毡桌垫|19.90|desk-mat.svg|Deskfolk|A 90cm felt desk mat with a soft surface that protects your desktop from everyday marks.|90 厘米毛毡桌垫，柔软表面保护桌面，减少日常划痕。
workspace|Cork Desk Mat|天然软木桌垫|26.00|desk-mat.svg|Deskfolk|An 80cm cork desk mat with a textured natural surface and a non-slip backing.|80 厘米天然软木桌垫，纹理细腻，配防滑底层。
workspace|Monitor Riser Shelf|桌面显示器增高架|39.00|monitor-riser.svg|Deskfolk|A monitor shelf that raises your screen and creates space underneath for a keyboard.|抬高显示器并在下方留出键盘收纳空间，让桌面更整洁。
audio|Studio Monitor Headphones|Studio 监听耳机|119.00|headphones.svg|Soundlane|Closed-back wired headphones with an over-ear fit and detachable cable for careful listening.|封闭式有线监听耳机，包耳设计与可拆卸线材适合专注聆听。
audio|Commute Wireless Headphones|Commute 无线通勤耳机|99.00|headphones.svg|Soundlane|Foldable Bluetooth headphones with a built-in microphone and a rechargeable battery.|可折叠蓝牙耳机，内置麦克风和充电电池，通勤携带方便。
audio|Work Call Headset|Work Call 通话耳麦|59.00|headphones.svg|Soundlane|A lightweight wired headset with a noise-reducing boom microphone for daily meetings.|轻量有线通话耳麦，配降噪麦克风，适合日常会议。
audio|Pocket Bluetooth Speaker|Pocket 口袋蓝牙音箱|29.90|bluetooth-speaker.svg|Wavefield|A palm-size Bluetooth speaker with a carrying loop for music around the home.|手掌大小蓝牙音箱，附便携挂绳，轻松随处听音乐。
audio|Room Stereo Speaker|Room 桌面立体声音箱|89.00|bluetooth-speaker.svg|Wavefield|A desktop Bluetooth speaker with two drivers and an auxiliary input for room-filling sound.|双扬声器桌面蓝牙音箱，支持 AUX 输入，声音饱满。
audio|Podcast USB Microphone|Podcast 播客麦克风|129.00|usb-microphone.svg|Wavefield|A USB podcast microphone with headphone monitoring, gain control and a tabletop stand.|USB 播客麦克风，支持耳机监听、增益调节，附桌面支架。
audio|Mini USB Microphone|Mini 迷你 USB 麦克风|49.00|usb-microphone.svg|Wavefield|A compact plug-and-play USB microphone with a folding stand for calls and recordings.|小巧即插即用 USB 麦克风，折叠支架适合通话与录音。
""";

    static List<Product> all() {
        return ROWS.lines().filter(line -> !line.isBlank()).map(line -> {
            String[] value = line.split("\\|", -1);
            Product product = new Product();
            product.setCategory(value[0]); product.setName(value[1]); product.setNameZh(value[2]);
            product.setPrice(new BigDecimal(value[3])); product.setImageUrl("/images/" + value[4]);
            product.setBrand(value[5]); product.setDescription(value[6]); product.setDescriptionZh(value[7]);
            product.setOrigin("Singapore"); product.setOriginZh("新加坡");
            return product;
        }).toList();
    }
}
