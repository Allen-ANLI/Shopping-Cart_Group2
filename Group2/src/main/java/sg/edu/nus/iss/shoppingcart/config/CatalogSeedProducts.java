package sg.edu.nus.iss.shoppingcart.config;

import sg.edu.nus.iss.shoppingcart.entity.Product;
import java.math.BigDecimal;
import java.util.List;

/** Local sample catalogue with fictional brands; sample reviews are separately labelled. */
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
storage|Portable SSD|便携固态硬盘 500GB|99.90|portable-ssd.svg|Bytewood|A 500GB portable solid-state drive with USB-C for documents, photos and projects.|500GB USB-C 便携固态硬盘，方便存储文档、照片与项目。
displays|Monitor|24 英寸办公显示器|199.90|monitor.svg|Bytewood|A 24-inch full-HD monitor with HDMI input and an adjustable tilt stand.|24 英寸全高清显示器，配备 HDMI 接口与可调倾斜支架。
audio|Bluetooth Speaker|便携蓝牙音箱|49.90|bluetooth-speaker.svg|Soundlane|A compact rechargeable Bluetooth speaker for music and podcasts at your desk.|可充电蓝牙音箱，桌面听歌与播客更方便。
workspace|Desk Mat|柔软防滑桌垫|12.90|desk-mat.svg|Deskfolk|A 70cm soft desk mat with a non-slip base for your keyboard and mouse.|70 厘米柔软桌垫，防滑底面让键盘与鼠标保持稳定。
audio|USB Microphone|桌面 USB 麦克风|89.90|usb-microphone.svg|Soundlane|A USB condenser microphone with a desktop stand and mute control for clear calls.|USB 电容麦克风带桌面支架和静音控制，通话录音更清晰。
displays|Studio 27 Monitor|Studio 27 英寸显示器|289.00|monitor.svg|Bytewood|A 27-inch QHD display with a slim bezel and HDMI input for spreadsheets and creative work.|27 英寸 QHD 窄边框显示器，适合表格处理和创意工作。
storage|Pocket SSD 1TB|Pocket 1TB 移动固态硬盘|149.00|portable-ssd.svg|Bytewood|A 1TB USB-C solid-state drive in an aluminium enclosure for large project libraries.|铝合金外壳的 1TB USB-C 固态硬盘，轻松容纳大型项目。
computing|Travel Hub 4-in-1|旅行四合一扩展坞|24.90|usb-c-hub.svg|Portwell|A lightweight four-port USB hub for connecting a keyboard, mouse and storage on the go.|轻便四口 USB 扩展坞，出行时连接键鼠与存储设备。
computing|Creator Hub 8-in-1|创作八合一扩展坞|69.00|usb-c-hub.svg|Portwell|An eight-port USB-C dock with HDMI, Ethernet and SD reader for a complete workspace.|八口 USB-C 扩展坞，含 HDMI、网口与 SD 读卡器。
computing|Focus Webcam 2K|Focus 2K 网络摄像头|89.00|webcam.svg|Portwell|A 2K webcam with a privacy cover and adjustable monitor mount for home meetings.|2K 网络摄像头带隐私遮挡盖与可调支架，适合居家会议。
storage|Archive SSD 2TB|Archive 2TB 固态硬盘|239.00|portable-ssd.svg|Bytewood|A roomy 2TB external SSD with USB-C for video archives and regular backups.|2TB 大容量 USB-C 移动固态硬盘，适合视频归档与日常备份。
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
computing|Airbook 14 Laptop|Airbook 14 英寸轻薄本|899.00|laptop.svg|Bytewood|A 14-inch laptop with 16GB memory and a 512GB SSD for everyday office work and study.|14 英寸轻薄本搭载 16GB 内存和 512GB 固态硬盘，适合日常办公与学习。
computing|Studio Mini PC|Studio 迷你台式电脑|699.00|mini-pc.svg|Bytewood|A compact desktop PC with 16GB memory, a 1TB SSD and dual-display output.|小巧台式电脑配备 16GB 内存、1TB 固态硬盘与双屏输出。
displays|Creator 32 4K Monitor|Creator 32 英寸 4K 显示器|499.00|monitor.svg|Bytewood|A 32-inch 4K display with USB-C input and adjustable height for detailed creative work.|32 英寸 4K 屏幕支持 USB-C 输入与高度调节，清晰展现创作细节。
displays|UltraWide 34 Monitor|UltraWide 34 英寸带鱼屏|349.00|monitor.svg|Bytewood|A 34-inch ultrawide display for side-by-side documents and a spacious timeline.|34 英寸超宽屏，方便并排查看文档与编辑时间轴。
displays|Travel 15 Portable Monitor|Travel 15 英寸便携屏|159.00|monitor.svg|Portwell|A lightweight 15.6-inch USB-C monitor with a folding cover for a mobile dual-screen setup.|轻便 15.6 英寸 USB-C 显示器配折叠保护套，随行开启双屏办公。
displays|Touch 24 Office Monitor|Touch 24 英寸触控屏|299.00|monitor.svg|Portwell|A 24-inch touch-enabled display with a tilt stand for interactive office workflows.|24 英寸触控显示器搭配倾斜支架，适合直观高效的办公操作。
storage|Everyday MicroSD 32GB|Everyday 32GB 存储卡|12.90|memory-card.svg|Bytewood|A 32GB microSD card with an adapter for documents, photos and compatible devices.|32GB microSD 存储卡附转接套，方便保存文档和照片。
storage|Slide USB Drive 128GB|Slide 128GB U 盘|19.90|usb-drive.svg|Bytewood|A 128GB USB flash drive with a retractable connector for carrying presentations.|128GB U 盘采用伸缩接口设计，演示文稿随身携带。
storage|Office NAS 2-Bay|Office 双盘位网络存储|699.00|nas.svg|Bytewood|A two-bay network storage enclosure for shared office files; drives sold separately.|双盘位网络存储机箱，便于团队共享办公文件；硬盘需另购。
storage|Backup HDD 4TB|Backup 4TB 桌面硬盘|139.00|portable-ssd.svg|Bytewood|A 4TB desktop hard drive with USB connectivity and an included power adapter.|4TB 桌面机械硬盘通过 USB 连接，附电源适配器，适合定期备份。
charging|GaN Charger 65W|65W 氮化镓快充头|39.90|charger.svg|Voltway|A compact 65W GaN charger with two USB-C ports and one USB-A port.|65W 小巧氮化镓充电器，提供两个 USB-C 接口和一个 USB-A 接口。
charging|Desk Charger 140W|140W 桌面多口充电站|89.00|charger.svg|Voltway|A 140W desktop charging station with four ports for a laptop and everyday devices.|140W 四口桌面充电站，为笔记本和日常数码设备集中供电。
charging|Power Bank 20000mAh|20000mAh 便携充电宝|49.90|power-bank.svg|Voltway|A 20000mAh power bank with USB-C charging and a clear battery level display.|20000mAh 大容量充电宝，支持 USB-C 充电并清晰显示剩余电量。
charging|Braided USB-C Cable 2m|2 米编织 USB-C 充电线|14.90|cable.svg|Voltway|A durable 2-metre USB-C charging cable rated for up to 100W with compatible devices.|耐用 2 米编织 USB-C 充电线，为兼容设备提供最高 100W 充电。
networking|Home Office Wi-Fi 6 Router|Home Office Wi-Fi 6 路由器|89.90|router.svg|Linkway|A dual-band Wi-Fi 6 router with four Ethernet ports for a small home office.|双频 Wi-Fi 6 路由器提供四个网口，适合居家办公空间。
networking|Mesh Wi-Fi 2-Pack|Mesh 双只装无线组网套装|179.00|mesh-router.svg|Linkway|Two mesh Wi-Fi nodes for consistent coverage across rooms and shared workspaces.|两只 Mesh 无线节点，改善多房间和共享办公空间的网络覆盖。
networking|Gigabit Switch 8-Port|八口千兆网络交换机|45.00|network-switch.svg|Linkway|An eight-port unmanaged Gigabit Ethernet switch with a quiet fanless enclosure.|八口千兆非网管交换机采用无风扇设计，安静扩展有线网络。
networking|USB-C Ethernet Adapter|USB-C 千兆网口转换器|24.90|usb-c-hub.svg|Portwell|A compact USB-C to Gigabit Ethernet adapter for stable wired video meetings.|小巧 USB-C 千兆网口转换器，为视频会议提供稳定的有线连接。
printing|Office Laser Printer|Office 黑白激光打印机|229.00|printer.svg|Paperflow|A monochrome laser printer with automatic duplex printing and wireless connectivity.|黑白激光打印机支持自动双面打印和无线连接，适合办公文档。
printing|Colour Inkjet All-in-One|彩色喷墨打印复印一体机|179.00|printer.svg|Paperflow|A colour inkjet printer, scanner and copier for home-office documents and images.|集彩色喷墨打印、扫描与复印于一体，适合居家办公文档和图片。
printing|Portable Document Scanner|便携文档扫描仪|129.00|scanner.svg|Paperflow|A USB-powered portable scanner for receipts, contracts and everyday paperwork.|USB 供电的便携扫描仪，方便整理票据、合同和日常纸质文档。
printing|Compact Label Printer|Compact 桌面标签打印机|59.90|label-printer.svg|Paperflow|A compact thermal label printer for organising files, drawers and office supplies.|小巧热敏标签打印机，轻松标记文件、抽屉和办公用品。
mobile|Slate 11 Tablet|Slate 11 英寸办公平板|349.00|tablet.svg|Bytewood|An 11-inch tablet with 128GB storage for note-taking, video meetings and reading.|11 英寸平板配备 128GB 存储空间，适合笔记、视频会议与阅读。
mobile|Slate Active Stylus|Slate 主动式触控笔|39.00|stylus.svg|Bytewood|A rechargeable active stylus with palm rejection for compatible Slate tablets.|可充电主动式触控笔，支持防误触，适用于兼容的 Slate 平板。
mobile|Everyday 5G Smartphone|Everyday 5G 智能手机|399.00|smartphone.svg|Bytewood|A 5G smartphone with 128GB storage and a 6.5-inch display for work and everyday life.|5G 智能手机配备 128GB 存储和 6.5 英寸屏幕，兼顾工作与生活。
mobile|Tablet Keyboard Folio|平板键盘保护套|69.00|tablet-keyboard.svg|Keycraft|A keyboard folio with a folding stand for compatible 11-inch tablets.|键盘保护套配折叠支架，适配兼容的 11 英寸平板电脑。
""";

    static List<Product> all() {
        return ROWS.lines().filter(line -> !line.isBlank()).map(line -> {
            String[] value = line.split("\\|", -1);
            Product product = new Product();
            product.setCategory(value[0]); product.setName(value[1]); product.setNameZh(value[2]);
            product.setPrice(new BigDecimal(value[3])); product.setImageUrl("/images/" + value[4]);
            product.setBrand(value[5]); product.setDescription(value[6]); product.setDescriptionZh(value[7]);
            product.setOrigin("Singapore"); product.setOriginZh("新加坡");
            // A curated mix of full-price products and realistic rotating promotions.
            int marker = Math.floorMod(product.getName().hashCode(), 10);
            product.setDiscountPercent(switch (marker) { case 0 -> 40; case 1 -> 30; case 2 -> 20; case 3 -> 15; case 4 -> 10; default -> 0; });
            return product;
        }).toList();
    }
}
