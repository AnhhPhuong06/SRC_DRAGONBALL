-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Aug 16, 2026 at 10:43 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `nro_acc`
--

-- --------------------------------------------------------

--
-- Table structure for table `account`
--

CREATE TABLE `account` (
  `id` int(11) NOT NULL,
  `username` varchar(20) NOT NULL,
  `password` varchar(100) NOT NULL,
  `email` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `create_time` timestamp NULL DEFAULT current_timestamp(),
  `update_time` timestamp NULL DEFAULT current_timestamp(),
  `ban` tinyint(1) NOT NULL DEFAULT 0,
  `is_admin` tinyint(1) NOT NULL DEFAULT 0,
  `last_time_login` timestamp NOT NULL DEFAULT '2002-07-30 17:00:00',
  `last_time_logout` timestamp NOT NULL DEFAULT '2002-07-30 17:00:00',
  `ip_address` varchar(50) DEFAULT NULL,
  `active` int(11) NOT NULL DEFAULT 0,
  `thoi_vang` int(11) NOT NULL DEFAULT 0,
  `server_login` int(11) NOT NULL DEFAULT -1,
  `bd_player` double DEFAULT 1,
  `is_gift_box` tinyint(1) DEFAULT 0,
  `gift_time` varchar(255) DEFAULT '0',
  `reward` longtext DEFAULT '[]',
  `cash` int(11) NOT NULL DEFAULT 0,
  `danap` int(11) NOT NULL DEFAULT 0,
  `mocnap` int(11) NOT NULL DEFAULT 0,
  `token` text NOT NULL,
  `xsrf_token` text NOT NULL,
  `newpass` text NOT NULL,
  `luotquay` int(11) NOT NULL DEFAULT 0,
  `vang` bigint(20) NOT NULL DEFAULT 0,
  `event_point` int(11) NOT NULL DEFAULT 0,
  `vip` int(11) NOT NULL DEFAULT 0,
  `vip1` int(11) NOT NULL DEFAULT 0,
  `vip2` int(11) NOT NULL DEFAULT 2,
  `sotien` int(11) NOT NULL DEFAULT 0,
  `diem_da_nhan` int(11) NOT NULL DEFAULT 0,
  `hasReceivedVIP` int(11) NOT NULL DEFAULT 0,
  `hasReceivedVIP1` int(11) NOT NULL DEFAULT 0,
  `hasReceivedVIP2` int(11) NOT NULL DEFAULT 0,
  `lastTimeReceivedVIP` bigint(20) NOT NULL DEFAULT 0,
  `lastTimeReceivedVIP1` bigint(20) NOT NULL DEFAULT 0,
  `lastTimeReceivedVIP2` bigint(20) NOT NULL DEFAULT 0,
  `coin` int(11) NOT NULL DEFAULT 0,
  `gioithieu` int(11) NOT NULL DEFAULT 0,
  `admin` int(11) NOT NULL DEFAULT 0,
  `tichdiem` int(11) NOT NULL DEFAULT 0,
  `mkc2` int(11) NOT NULL DEFAULT 0,
  `gmail` varchar(225) NOT NULL,
  `server` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `account`
--

-- Private/operational rows omitted: account


-- --------------------------------------------------------

--
-- Table structure for table `adminpanel`
--

CREATE TABLE `adminpanel` (
  `id` int(11) NOT NULL,
  `domain` varchar(255) NOT NULL,
  `logo` varchar(255) DEFAULT NULL,
  `trangthai` enum('bao_tri','hoat_dong') NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `android` varchar(255) DEFAULT NULL,
  `iphone` varchar(255) DEFAULT NULL,
  `windows` varchar(255) DEFAULT NULL,
  `java` varchar(255) DEFAULT NULL,
  `apikey` text NOT NULL,
  `taikhoanmb` text NOT NULL,
  `stkmb` text NOT NULL,
  `tenmb` text NOT NULL,
  `time_reset_moc_nap` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `clan`
--

CREATE TABLE `clan` (
  `id` int(11) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `NAME_2` varchar(4) NOT NULL,
  `slogan` varchar(255) NOT NULL DEFAULT '',
  `img_id` int(11) NOT NULL DEFAULT 0,
  `power_point` bigint(20) NOT NULL DEFAULT 0,
  `max_member` smallint(6) NOT NULL DEFAULT 10,
  `clan_point` int(11) NOT NULL DEFAULT 0,
  `LEVEL` int(11) NOT NULL DEFAULT 1,
  `members` text NOT NULL,
  `tops` text NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `clan`
--

-- Private/operational rows omitted: clan


-- --------------------------------------------------------

--
-- Table structure for table `event`
--

CREATE TABLE `event` (
  `id` int(11) NOT NULL,
  `name` text NOT NULL,
  `data` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `event`
--

INSERT INTO `event` (`id`, `name`, `data`) VALUES
(1, 'LUNNAR_NEW_YEAR', '{\"damePrecent\":0,\"hpPrecent\":0,\"mpPrecent\":0,\"papPrecent\":0}');

-- --------------------------------------------------------

--
-- Table structure for table `giftcode`
--

CREATE TABLE `giftcode` (
  `id` int(11) NOT NULL,
  `code` text NOT NULL,
  `count_left` int(11) NOT NULL,
  `detail` text NOT NULL,
  `allGender` text NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `expired` timestamp NOT NULL DEFAULT current_timestamp(),
  `player_name` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `giftcode`
--

-- Private/operational rows omitted: giftcode


-- --------------------------------------------------------

--
-- Table structure for table `history_active`
--

CREATE TABLE `history_active` (
  `id` int(11) NOT NULL,
  `username` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `history_active`
--

-- Private/operational rows omitted: history_active


-- --------------------------------------------------------

--
-- Table structure for table `history_transaction`
--

CREATE TABLE `history_transaction` (
  `id` int(11) NOT NULL,
  `player_1` varchar(255) NOT NULL,
  `player_2` varchar(255) NOT NULL,
  `item_player_1` text NOT NULL,
  `item_player_2` text NOT NULL,
  `bag_1_before_tran` text NOT NULL,
  `bag_2_before_tran` text NOT NULL,
  `bag_1_after_tran` text NOT NULL,
  `bag_2_after_tran` text NOT NULL,
  `time_tran` timestamp NOT NULL DEFAULT current_timestamp(),
  `detail_gold_1` text DEFAULT NULL,
  `detail_gold_2` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `mbbank_log`
--

CREATE TABLE `mbbank_log` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `benAccountName` varchar(255) NOT NULL,
  `accountNo` varchar(255) NOT NULL,
  `bankName` varchar(255) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `date` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE `messages` (
  `id` int(11) NOT NULL,
  `username` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `created_at` varchar(999) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `momo_trans`
--

CREATE TABLE `momo_trans` (
  `ID` int(11) NOT NULL,
  `tranId` varchar(255) NOT NULL,
  `io` varchar(255) NOT NULL,
  `partnerId` varchar(255) NOT NULL,
  `status` int(11) NOT NULL,
  `partnerName` varchar(255) NOT NULL,
  `amount` int(11) NOT NULL,
  `comment` varchar(255) NOT NULL,
  `millisecond` varchar(255) NOT NULL,
  `date` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `napthe`
--

CREATE TABLE `napthe` (
  `id` int(11) NOT NULL,
  `user_nap` varchar(100) NOT NULL,
  `telco` varchar(255) NOT NULL,
  `serial` varchar(255) NOT NULL,
  `code` varchar(255) NOT NULL,
  `amount` int(11) NOT NULL,
  `status` int(11) NOT NULL,
  `request_id` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `naptien`
--

CREATE TABLE `naptien` (
  `id` int(11) NOT NULL,
  `uid` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` text NOT NULL,
  `sotien` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `seri` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `loaithe` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time` timestamp NULL DEFAULT current_timestamp(),
  `noidung` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tinhtrang` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tranid` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `magioithieu` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--

CREATE TABLE `payments` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(50) DEFAULT NULL,
  `refNo` varchar(255) NOT NULL,
  `date` text NOT NULL,
  `amount` int(11) NOT NULL,
  `status` varchar(50) NOT NULL,
  `bank` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `player`
--

CREATE TABLE `player` (
  `id` int(11) NOT NULL,
  `account_id` int(11) DEFAULT NULL,
  `name` varchar(20) NOT NULL,
  `info` text NOT NULL,
  `head` int(11) NOT NULL DEFAULT 102,
  `gender` int(11) NOT NULL,
  `have_tennis_space_ship` tinyint(1) DEFAULT 0,
  `clan_id` int(11) NOT NULL DEFAULT -1,
  `data_inventory` text NOT NULL,
  `data_location` text NOT NULL,
  `data_point` text NOT NULL,
  `data_magic_tree` text NOT NULL,
  `items_body` text NOT NULL,
  `items_bag` text NOT NULL,
  `items_box` text NOT NULL,
  `items_box_lucky_round` text NOT NULL,
  `items_daban` text NOT NULL,
  `friends` text NOT NULL,
  `enemies` text NOT NULL,
  `data_intrinsic` text NOT NULL,
  `data_item_time` text NOT NULL,
  `devndung_time` text NOT NULL,
  `data_task` text NOT NULL,
  `data_mabu_egg` text NOT NULL,
  `bill_data` text NOT NULL,
  `black_data` text NOT NULL DEFAULT '[]',
  `data_charm` text NOT NULL,
  `skills` text NOT NULL,
  `skills_shortcut` text NOT NULL,
  `pet` text NOT NULL,
  `data_black_ball` text NOT NULL,
  `data_side_task` text NOT NULL,
  `data_danh_hieu` text NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT current_timestamp(),
  `notify` text CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `baovetaikhoan` varchar(1000) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL DEFAULT '[]',
  `captcha` varchar(1000) NOT NULL DEFAULT '[]',
  `data_card` varchar(10000) NOT NULL DEFAULT '[]',
  `lasttimepkcommeson` bigint(20) NOT NULL DEFAULT 0,
  `bandokhobau` varchar(250) NOT NULL DEFAULT '[]',
  `conduongrandoc` varchar(255) NOT NULL DEFAULT '[]',
  `doanhtrai` varchar(250) NOT NULL DEFAULT '[]',
  `masterDoesNotAttack` text NOT NULL,
  `nhanthoivang` varchar(200) NOT NULL DEFAULT '[]',
  `ruonggo` varchar(255) NOT NULL DEFAULT '[]',
  `sieuthanthuy` varchar(255) NOT NULL DEFAULT '[]',
  `vodaisinhtu` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]',
  `rongxuong` bigint(20) NOT NULL DEFAULT 0,
  `data_item_event` varchar(1000) NOT NULL DEFAULT '[]',
  `data_luyentap` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `data_clan_task` varchar(255) NOT NULL DEFAULT '[]',
  `data_vip` text DEFAULT NULL,
  `rank` int(11) NOT NULL DEFAULT 0,
  `data_achievement` text NOT NULL,
  `giftcode` text NOT NULL,
  `danh_hieu_shop` varchar(50) NOT NULL DEFAULT '[0,0,0,0,0,0,0,0]',
  `data_clan` text DEFAULT NULL,
  `firstTimeLogin` timestamp NOT NULL DEFAULT current_timestamp(),
  `buarandom` varchar(50) NOT NULL DEFAULT '[1,1]',
  `dien_sukien` varchar(255) NOT NULL DEFAULT '[0,0,0]',
  `banhtet` bigint(20) NOT NULL DEFAULT 0,
  `banhchung` bigint(20) NOT NULL DEFAULT 0,
  `hoc_ky_nang` longtext DEFAULT NULL,
  `boughtSkills` longtext DEFAULT NULL,
  `data_KOL` text NOT NULL,
  `loadtimetop` text NOT NULL,
  `point_pokemon` varchar(11) NOT NULL DEFAULT '[0]',
  `daubuoi` int(11) NOT NULL DEFAULT 0,
  `sanboss` bigint(20) DEFAULT 0,
  `jiren_data` text NOT NULL DEFAULT '[]'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `player`
--

-- Private/operational rows omitted: player


-- --------------------------------------------------------

--
-- Table structure for table `player_gift_data`
--

CREATE TABLE `player_gift_data` (
  `player_id` bigint(20) NOT NULL,
  `data_gift` mediumtext NOT NULL,
  `update_time` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `posts`
--

CREATE TABLE `posts` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `comments` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `image` varchar(255) DEFAULT NULL,
  `slug` varchar(255) DEFAULT NULL,
  `content` text DEFAULT NULL,
  `views` int(11) DEFAULT 0,
  `order_number` int(11) DEFAULT 0,
  `type` varchar(50) DEFAULT 'article',
  `author` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `posts`
--

-- Private/operational rows omitted: posts


-- --------------------------------------------------------

--
-- Table structure for table `recharge`
--

CREATE TABLE `recharge` (
  `id` int(11) NOT NULL,
  `account_id` varchar(999) NOT NULL,
  `code` varchar(999) NOT NULL,
  `serial` varchar(999) NOT NULL,
  `amount` varchar(999) NOT NULL,
  `type` varchar(999) NOT NULL,
  `tranid` varchar(999) NOT NULL,
  `amount_real` varchar(999) NOT NULL,
  `status` int(11) NOT NULL,
  `time` varchar(999) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `sell_item`
--

CREATE TABLE `sell_item` (
  `id` int(11) NOT NULL,
  `price` int(11) NOT NULL,
  `item` int(11) NOT NULL,
  `slot` int(11) NOT NULL,
  `options` text NOT NULL,
  `users_buy` text NOT NULL,
  `status` int(11) NOT NULL,
  `time` varchar(999) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `setting`
--

CREATE TABLE `setting` (
  `id` int(11) NOT NULL,
  `title` varchar(999) NOT NULL,
  `description` varchar(999) NOT NULL,
  `author` varchar(999) NOT NULL,
  `keywords` varchar(999) NOT NULL,
  `logo` varchar(999) NOT NULL,
  `background` varchar(999) NOT NULL,
  `size_logo` int(11) NOT NULL,
  `banner` varchar(999) NOT NULL,
  `navbar` varchar(999) NOT NULL,
  `download` text NOT NULL,
  `mtv` varchar(999) DEFAULT NULL,
  `amount_mtv` varchar(999) NOT NULL DEFAULT '1',
  `thongbao` varchar(99) NOT NULL,
  `nd_thongbao` text NOT NULL,
  `favicon` varchar(999) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `setting`
--

-- Private/operational rows omitted: setting


-- --------------------------------------------------------

--
-- Table structure for table `shop_ky_gui`
--

CREATE TABLE `shop_ky_gui` (
  `id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `tab` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  `gold` int(11) NOT NULL,
  `ruby` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `itemOption` text NOT NULL,
  `lasttime` bigint(20) NOT NULL,
  `isBuy` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Table structure for table `super_rank`
--

CREATE TABLE `super_rank` (
  `id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `name` text NOT NULL,
  `rank` int(11) NOT NULL,
  `last_pk_time` bigint(20) NOT NULL,
  `last_reward_time` bigint(20) NOT NULL,
  `ticket` int(11) NOT NULL,
  `win` int(11) NOT NULL,
  `lose` int(11) NOT NULL,
  `history` text NOT NULL,
  `info` text NOT NULL,
  `received` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `super_rank`
--

-- Private/operational rows omitted: super_rank

-- Private/operational rows omitted: super_rank

-- Private/operational rows omitted: super_rank


-- --------------------------------------------------------

--
-- Table structure for table `task_main_template`
--

CREATE TABLE `task_main_template` (
  `id` int(11) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `detail` varchar(500) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `task_main_template`
--

INSERT INTO `task_main_template` (`id`, `NAME`, `detail`) VALUES
(0, 'Nhiệm vụ đầu tiên', 'Chi tiết nhiệm vụ'),
(1, 'Nhiệm vụ tập luyện', 'Mộc nhân được đặt nhiều tại %1, ngay trước nhà %2\nHãy đánh ngã 5 mộc nhân, \nsau đó quay về nhà báo cáo với ông %2\nĐể đánh, hãy chạm nhanh 2 lần vào đối tượng\nThưởng 1000 sức mạnh\nThưởng 1000 tiềm năng\nThưởng 200tr vàng'),
(2, 'Nhiệm vụ tìm thức ăn', 'Tìm đến %3, tiêu diệt bọn quái %4 và nhặt về 10 đùi gà\nThưởng 1500 sức mạnh\nThưởng 1500 tiềm năng\nThưởng 300tr vàng\nHọc được kỹ năng bay'),
(3, 'Nhiệm vụ sao băng', 'Đi khám phá xem vật thể lạ vừa rơi xuống hành tinh\nThưởng 2000 sức mạnh\nThưởng 2000 tiềm năng\nThưởng 400tr vàng'),
(4, 'Nhiệm vụ thử thách', 'Khủng long mẹ sống tại Trái Đất\nLợn lòi mẹ sống tại Namếc\nQuỷ đất mẹ sống tại Xayda\nDùng tàu vũ trụ để di chuyển sang hành\nkhác\n-Thưởng 4.000 sức mạnh\n-Thưởng 4.000 tiềm năng'),
(5, 'Nhiệm vụ thử thách', 'Lợn lòi mẹ sống tại Namếc\nKhủng long mẹ sống tại Trái Đất\nQuỷ đất mẹ sống tại Xayda\nDùng tàu vũ trụ để di chuyển sang hành\nkhác\n-Thưởng 4.000 sức mạnh\n-Thưởng 4.000 tiềm năng'),
(6, 'Nhiệm vụ thử thách', 'Quỷ đất mẹ sống tại Xayda\nKhủng long mẹ sống tại Trái Đất\nLợn lòi mẹ sống tại Namếc\nDùng tàu vũ trụ để di chuyển sang hành\nkhác\n-Thưởng 4.000 sức mạnh\n-Thưởng 4.000 tiềm năng'),
(7, 'Nhiệm vụ giải cứu', 'Đến khu vực %13,\nHạ 20 con %9\n- Thưởng 8.000 sức mạnh\n- Thưởng 8.000 tiềm năng'),
(8, 'Nhiệm vụ tìm ngọc', 'Ngọc rồng 7 sao đang bị bọn\n%14 cướp đi.\nĐánh bại chúng để tìm lại.\n- Thưởng 15.000 sức mạnh\n- Thưởng 15.000 tiềm năng'),
(9, 'Nhiệm vụ bái sư', 'Tìm đường tới %11, trò chuyện với %10 và xin làm đệ tử'),
(10, 'Nhiệm vụ thử sức', 'Tiêu diệt %12 thể hiện sức mạnh cho %10 thấy'),
(11, 'Nhiệm vụ làm quen các Npc', 'Hãy gặp và làm quen với các Npc để biết chức năng của họ'),
(12, 'Nhiệm vụ xin phép', 'Quay về nhà xin %2 cho phép lên đường trở thành chiến binh'),
(13, 'Nhiệm vụ nhận quà', 'Gặp Npc Quy Lão nhận ngay đuôi khỉ đi úp cho phê'),
(14, 'Nhiệm vụ bang hội đầu tiên', 'Tiến trình sẽ nhanh gấp đôi nếu cùng phối hợp với 1 người đồng đội lên đường làm nhiệm vụ\r\nGợi ý:\r\nHeo rừng xuất hiện tại rừng Bamboo\r\nHeo da xanh xuất \r\nhiện tại núi hoa vàng\r\nHeo xayda xuất hiện tại rừng cọ\r\nHãy tới trạm tàu vũ trụ để có thể di chuyển qua các map'),
(15, 'Nhiệm vụ bang hội thứ 2', 'Tiến trình sẽ nhanh gấp đôi nếu cùng phối hợp với 1 người đồng đội lên đường làm nhiệm vụ'),
(16, 'Nhiệm vụ tiêu diệt quái vật', 'Tới các hành tinh tiêu diệt quái vật, giải cứu thường dân'),
(17, 'Nhiệm vụ giúp đỡ Cui', 'Tìm đường tới thành phố Vegeta, gặp và nói chuyện với Cui'),
(18, 'Nhiệm vụ bất khả thi', 'Đạt 50 triệu sức mạnh\nTiêu diệt bọn tay sai của Fide tại Xayda\n- Thưởng 50.000.000 sức mạnh\n- Thưởng 50.000.000 tiềm năng'),
(19, 'Nhiệm vụ tìm diệt đệ tử', 'Tiêu diệt bọn đệ tử Kuku, Mập Đầu Đinh,\nRambo của Fide đại ca tại Xayda\nCui có thể biết vị trí của chúng, nếu tìm\nkhông thấy hãy đến gặp Cui tại thành\nphố Vegeta\n- Thưởng 20.000.000 sức mạnh\n- Thưởng 20.000.000 tiềm năng'),
(20, 'Nhiệm vụ Tiểu đội sát thủ', 'Tiêu diệt Tiểu Đội Sát Thủ do Fide đại\nca gọi đến tại Xayda\n- Thưởng 20.000.000 sức mạnh\n- Thưởng 20.000.000 tiềm năng'),
(21, 'Nhiệm vụ chạm trán Fide đại ca', 'Luyện tập đạt 2 tỷ sức mạnh\nLên đường tìm diệt Fide đại ca'),
(22, 'Chú bé đến từ tương lai', 'Đến trái đất, rừng bamboo, rừng dương\nxỉ, nam Kamê tìm người lạ\nĐến đảo rùa đưa thuốc cho Quy Lão\nTheo Ca Lích đến tương lai\nGiúp họ diệt bọn bọ hung con\n- Thưởng 1.000.000 sức mạnh\n- Thưởng 1.000.000 tiềm năng'),
(23, 'Chạm chán Robot sát thủ lần 1', 'Hãy đến thành phố phía nam\nđảo balê hoặc cao nguyên\nCùng 2 đồng bang diệt 1300 Xên con cấp 3\nBáo với Bunma tương lai\n- Thưởng 1.000.000 sức mạnh\n- Thưởng 1.000.000 tiềm năng'),
(24, 'Chạm trán Robot sát thủ lần 2', 'Trở về quá khứ, đến sân sau siêu thị\nTiêu diệt bọn Rôbốt sát thủ\nBáo với Bunma tương lai\n- Thưởng 1.000.000 sức mạnh\n- Thưởng 1.000.000 tiềm năng'),
(25, 'Chạm trán Robot sát thủ lần 3', 'Đến thành phố, ngọn núi, thung lũng phía Bắc\nTiêu diệt bọn Rôbốt sát thủ\nCùng 2 đồng bang diệt 1500 Xên con cấp 5\nBáo với Bunma tương lai\n- Thưởng 1.000.000 sức mạnh\n- Thưởng 1.000.000 tiềm năng'),
(26, 'Chạm trán Xên bọ hung', 'Đến thị trấn Ginder\nTiêu diệt Xên Bọ Hung cấp 1\nTiêu diệt Xên Bọ Hung cấp 2\nTiêu diệt Xên Bọ Hung hoàn thiện\nCùng 2 đồng bang diệt 1500 Xên con cấp 8\nBáo với Bunma tương lai\n- Thưởng 1.000.000 sức mạnh\n- Thưởng 1.000.000 tiềm năng'),
(27, 'Cuộc dạo chơi của xên', 'Cẩn thận !!!\nNhững vị khách không mời mà tới\nthường tỏ ra nguy hiểm'),
(28, 'Cuộc đối đầu không cân sức', 'Vào lúc 12h trưa các ngày, bạn đến gặp NPC Ô sin tại map Đại hội võ thuật.'),
(29, 'Hành tinh băng giá', 'Đến Hang Băng\nTiêu diệt cooler'),
(30, 'Hành tinh ngục tù', 'Hành tinh ngục tù'),
(31, 'Truyền Thuyết Về Trùm Server', 'Ai mới là trùm server?'),
(32, 'Truyền thuyết về Siêu Xayda Huyền Thoại', 'Ai mới thật sự là siêu xayda huyền thoại mà Fide từng nhắc tới ?\nThưởng 10 Tr sức mạnh\nThưởng 10 Tr tiềm năng'),
(33, 'Ta sẽ trở thành TOP nhiệm vụ Server NRO Xưa', 'Cần cù bù thông minh'),
(34, 'Làm nhiệm vụ, làm nhiệm vụ nữa, làm nhiệm vụ mãi', 'Đừng lo lắng rồi mọi chuyện cũng sẽ như đb mà thôi');

-- --------------------------------------------------------

--
-- Table structure for table `top_template`
--

CREATE TABLE `top_template` (
  `id` int(11) NOT NULL,
  `name` varchar(25) NOT NULL,
  `query` text NOT NULL,
  `date` datetime NOT NULL,
  `items` text NOT NULL,
  `users` text NOT NULL,
  `isDone` varchar(5) NOT NULL DEFAULT '0',
  `isAuto` varchar(5) NOT NULL DEFAULT '0',
  `limit` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

--
-- Dumping data for table `top_template`
--

INSERT INTO `top_template` (`id`, `name`, `query`, `date`, `items`, `users`, `isDone`, `isAuto`, `limit`) VALUES
(0, 'Nạp', 'SELECT player.id as id, account.danap AS tongnap FROM player INNER JOIN account ON account.id = player.account_id WHERE account.danap > 0 AND account.is_admin = 0 AND account.ban = 0 ORDER BY tongnap DESC LIMIT 100', '2029-12-28 23:59:59', '[{\"top\":1,\r\n\"items\":[{\"id\":1994,\"quantity\":1,\"options\":[{\"id\":50,\"param\":35},{\"id\":77,\"param\":35},{\"id\":103,\"param\":35}]},\r\n{\"id\":1962,\"quantity\":1,\"options\":[{\"id\":50,\"param\":50},{\"id\":77,\"param\":55},{\"id\":103,\"param\":55},{\"id\":5,\"param\":50}]},\r\n{\"id\":1324,\"quantity\":20,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1946,\"quantity\":1,\"options\":[{\"id\":50,\"param\":45},{\"id\":77,\"param\":55},{\"id\":103,\"param\":55},{\"id\":5,\"param\":45},{\"id\":14,\"param\":25},{\"id\":30,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":50,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":50,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":3000,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":2,\r\n\"items\":[{\"id\":1993,\"quantity\":1,\"options\":[{\"id\":50,\"param\":30},{\"id\":77,\"param\":30},{\"id\":103,\"param\":30}]},\r\n{\"id\":1227,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1324,\"quantity\":2,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1241,\"quantity\":1,\"options\":[{\"id\":50,\"param\":44},{\"id\":77,\"param\":44},{\"id\":103,\"param\":44},{\"id\":5,\"param\":34},{\"id\":14,\"param\":24},{\"id\":30,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":30,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":30,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":2000,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":3,\r\n\"items\":[{\"id\":1992,\"quantity\":1,\"options\":[{\"id\":50,\"param\":25},{\"id\":77,\"param\":25},{\"id\":103,\"param\":25}]},\r\n{\"id\":1227,\"quantity\":3,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1324,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1241,\"quantity\":1,\"options\":[{\"id\":50,\"param\":42},{\"id\":77,\"param\":42},{\"id\":103,\"param\":42},{\"id\":5,\"param\":32},{\"id\":14,\"param\":22},{\"id\":30,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":20,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":20,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":1000,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":4,\r\n\"items\":[{\"id\":457,\"quantity\":400,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":5,\r\n\"items\":[{\"id\":457,\"quantity\":400,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":6,\r\n\"items\":[{\"id\":457,\"quantity\":400,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":7,\r\n\"items\":[{\"id\":457,\"quantity\":400,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":8,\r\n\"items\":[{\"id\":457,\"quantity\":400,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":9,\r\n\"items\":[{\"id\":457,\"quantity\":400,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":10,\r\n\"items\":[{\"id\":457,\"quantity\":400,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1305,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]}]', '[]', 'false', 'true', 10),
(1, 'Sức Mạnh', 'SELECT player.id as id, CAST(JSON_UNQUOTE(JSON_EXTRACT(data_point, \'$[1]\')) AS UNSIGNED) AS sm FROM player INNER JOIN account on account.id = player.account_id WHERE JSON_EXTRACT(data_point, \'$[1]\') > 2000 AND account.is_admin = 0 AND account.ban = 0 ORDER BY sm DESC LIMIT 100', '2029-12-28 23:59:59', '[{\"top\":1,\r\n\"items\":[{\"id\":1409,\"quantity\":1,\"options\":[{\"id\":50,\"param\":18},{\"id\":77,\"param\":18},{\"id\":103,\"param\":18}]},\r\n{\"id\":1227,\"quantity\":3,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1324,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1112,\"quantity\":1,\"options\":[{\"id\":50,\"param\":39},{\"id\":77,\"param\":39},{\"id\":103,\"param\":39},{\"id\":5,\"param\":29},{\"id\":14,\"param\":19},{\"id\":30,\"param\":0}]},\r\n{\"id\":14,\"quantity\":3,\"options\":[]},\r\n{\"id\":15,\"quantity\":3,\"options\":[]},\r\n{\"id\":16,\"quantity\":3,\"options\":[]},\r\n{\"id\":17,\"quantity\":3,\"options\":[]},\r\n{\"id\":18,\"quantity\":3,\"options\":[]},\r\n{\"id\":19,\"quantity\":3,\"options\":[]},\r\n{\"id\":20,\"quantity\":3,\"options\":[]},\r\n{\"id\":1199,\"quantity\":30,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":500,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":2,\r\n\"items\":[{\"id\":1410,\"quantity\":1,\"options\":[{\"id\":50,\"param\":13},{\"id\":77,\"param\":13},{\"id\":103,\"param\":13}]},\r\n{\"id\":1227,\"quantity\":2,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1324,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1112,\"quantity\":1,\"options\":[{\"id\":50,\"param\":38},{\"id\":77,\"param\":38},{\"id\":103,\"param\":38},{\"id\":5,\"param\":28},{\"id\":14,\"param\":18},{\"id\":30,\"param\":0}]},\r\n{\"id\":14,\"quantity\":2,\"options\":[]},\r\n{\"id\":15,\"quantity\":2,\"options\":[]},\r\n{\"id\":16,\"quantity\":2,\"options\":[]},\r\n{\"id\":17,\"quantity\":2,\"options\":[]},\r\n{\"id\":18,\"quantity\":2,\"options\":[]},\r\n{\"id\":19,\"quantity\":2,\"options\":[]},\r\n{\"id\":20,\"quantity\":2,\"options\":[]},\r\n{\"id\":1199,\"quantity\":10,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":300,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":3,\r\n\"items\":[{\"id\":1410,\"quantity\":1,\"options\":[{\"id\":50,\"param\":10},{\"id\":77,\"param\":10},{\"id\":103,\"param\":10}]},\r\n{\"id\":1227,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1324,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1112,\"quantity\":1,\"options\":[{\"id\":50,\"param\":37},{\"id\":77,\"param\":37},{\"id\":103,\"param\":37},{\"id\":5,\"param\":27},{\"id\":14,\"param\":17},{\"id\":30,\"param\":0}]},\r\n{\"id\":14,\"quantity\":1,\"options\":[]},\r\n{\"id\":15,\"quantity\":1,\"options\":[]},\r\n{\"id\":16,\"quantity\":1,\"options\":[]},\r\n{\"id\":17,\"quantity\":1,\"options\":[]},\r\n{\"id\":18,\"quantity\":1,\"options\":[]},\r\n{\"id\":19,\"quantity\":1,\"options\":[]},\r\n{\"id\":20,\"quantity\":1,\"options\":[]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":200,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":4,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":5,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":6,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":7,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":8,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":9,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":10,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]}]', '[]', 'false', 'true', 10),
(2, 'Nhiệm Vụ', 'SELECT player.id as id, CAST(JSON_UNQUOTE(JSON_EXTRACT(data_task, \'$[0]\')) AS UNSIGNED) AS nv, CAST(JSON_UNQUOTE(JSON_EXTRACT(data_task, \'$[1]\')) AS UNSIGNED) AS nvp, CAST(JSON_UNQUOTE(JSON_EXTRACT(data_task, \'$[2]\')) AS UNSIGNED) AS count, CAST(JSON_UNQUOTE(JSON_EXTRACT(data_task, \'$[3]\')) AS UNSIGNED) AS time FROM player INNER JOIN account on account.id = player.account_id WHERE JSON_EXTRACT(data_task, \'$[0]\') > 1 AND account.is_admin = 0 AND account.ban = 0 ORDER BY nv DESC, nvp DESC, count DESC, time ASC LIMIT 100', '2029-12-28 23:59:59', '[{\"top\":1,\r\n\"items\":[{\"id\":1409,\"quantity\":1,\"options\":[{\"id\":50,\"param\":18},{\"id\":77,\"param\":18},{\"id\":103,\"param\":18}]},\r\n{\"id\":1227,\"quantity\":3,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1324,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1112,\"quantity\":1,\"options\":[{\"id\":50,\"param\":39},{\"id\":77,\"param\":39},{\"id\":103,\"param\":39},{\"id\":5,\"param\":29},{\"id\":14,\"param\":19},{\"id\":30,\"param\":0}]},\r\n{\"id\":14,\"quantity\":3,\"options\":[]},\r\n{\"id\":15,\"quantity\":3,\"options\":[]},\r\n{\"id\":16,\"quantity\":3,\"options\":[]},\r\n{\"id\":17,\"quantity\":3,\"options\":[]},\r\n{\"id\":18,\"quantity\":3,\"options\":[]},\r\n{\"id\":19,\"quantity\":3,\"options\":[]},\r\n{\"id\":20,\"quantity\":3,\"options\":[]},\r\n{\"id\":1199,\"quantity\":30,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":500,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":2,\r\n\"items\":[{\"id\":1410,\"quantity\":1,\"options\":[{\"id\":50,\"param\":13},{\"id\":77,\"param\":13},{\"id\":103,\"param\":13}]},\r\n{\"id\":1227,\"quantity\":2,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1324,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1112,\"quantity\":1,\"options\":[{\"id\":50,\"param\":38},{\"id\":77,\"param\":38},{\"id\":103,\"param\":38},{\"id\":5,\"param\":28},{\"id\":14,\"param\":18},{\"id\":30,\"param\":0}]},\r\n{\"id\":14,\"quantity\":2,\"options\":[]},\r\n{\"id\":15,\"quantity\":2,\"options\":[]},\r\n{\"id\":16,\"quantity\":2,\"options\":[]},\r\n{\"id\":17,\"quantity\":2,\"options\":[]},\r\n{\"id\":18,\"quantity\":2,\"options\":[]},\r\n{\"id\":19,\"quantity\":2,\"options\":[]},\r\n{\"id\":20,\"quantity\":2,\"options\":[]},\r\n{\"id\":1199,\"quantity\":10,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":300,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":3,\r\n\"items\":[{\"id\":1410,\"quantity\":1,\"options\":[{\"id\":50,\"param\":10},{\"id\":77,\"param\":10},{\"id\":103,\"param\":10}]},\r\n{\"id\":1227,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1324,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":1112,\"quantity\":1,\"options\":[{\"id\":50,\"param\":37},{\"id\":77,\"param\":37},{\"id\":103,\"param\":37},{\"id\":5,\"param\":27},{\"id\":14,\"param\":17},{\"id\":30,\"param\":0}]},\r\n{\"id\":14,\"quantity\":1,\"options\":[]},\r\n{\"id\":15,\"quantity\":1,\"options\":[]},\r\n{\"id\":16,\"quantity\":1,\"options\":[]},\r\n{\"id\":17,\"quantity\":1,\"options\":[]},\r\n{\"id\":18,\"quantity\":1,\"options\":[]},\r\n{\"id\":19,\"quantity\":1,\"options\":[]},\r\n{\"id\":20,\"quantity\":1,\"options\":[]},\r\n{\"id\":1199,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]},\r\n{\"id\":457,\"quantity\":200,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]}]},\r\n{\"top\":4,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":5,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":6,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":7,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":8,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":9,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]},\r\n{\"top\":10,\r\n\"items\":[{\"id\":457,\"quantity\":100,\"options\":[{\"id\":100,\"param\":1},{\"id\":86,\"param\":0}]},\r\n{\"id\":1200,\"quantity\":5,\"options\":[{\"id\":30,\"param\":0}]}]}]', '[]', 'false', 'true', 10);

-- --------------------------------------------------------

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `username` varchar(100) DEFAULT NULL,
  `amount` int(11) NOT NULL DEFAULT 0,
  `type` varchar(50) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `trans_log`
--

CREATE TABLE `trans_log` (
  `id` int(11) NOT NULL,
  `name` text NOT NULL,
  `amount` bigint(20) NOT NULL,
  `seri` text NOT NULL,
  `pin` text NOT NULL,
  `type` text NOT NULL,
  `status` int(11) NOT NULL DEFAULT 0,
  `trans_id` text NOT NULL,
  `date` timestamp NOT NULL DEFAULT current_timestamp(),
  `giatri` int(11) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci ROW_FORMAT=DYNAMIC;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `account`
--
ALTER TABLE `account`
  ADD PRIMARY KEY (`id`) USING BTREE,
  ADD UNIQUE KEY `username` (`username`) USING BTREE;

--
-- Indexes for table `adminpanel`
--
ALTER TABLE `adminpanel`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `clan`
--
ALTER TABLE `clan`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `event`
--
ALTER TABLE `event`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `giftcode`
--
ALTER TABLE `giftcode`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `history_active`
--
ALTER TABLE `history_active`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `history_transaction`
--
ALTER TABLE `history_transaction`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `mbbank_log`
--
ALTER TABLE `mbbank_log`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `momo_trans`
--
ALTER TABLE `momo_trans`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indexes for table `napthe`
--
ALTER TABLE `napthe`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `naptien`
--
ALTER TABLE `naptien`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`id`) USING BTREE,
  ADD UNIQUE KEY `uniq_refno` (`refNo`);

--
-- Indexes for table `player`
--
ALTER TABLE `player`
  ADD PRIMARY KEY (`id`) USING BTREE,
  ADD UNIQUE KEY `account_id` (`account_id`) USING BTREE;

--
-- Indexes for table `player_gift_data`
--
ALTER TABLE `player_gift_data`
  ADD PRIMARY KEY (`player_id`);

--
-- Indexes for table `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `recharge`
--
ALTER TABLE `recharge`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `sell_item`
--
ALTER TABLE `sell_item`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `setting`
--
ALTER TABLE `setting`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `shop_ky_gui`
--
ALTER TABLE `shop_ky_gui`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `super_rank`
--
ALTER TABLE `super_rank`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `task_main_template`
--
ALTER TABLE `task_main_template`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- Indexes for table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_status_created` (`status`,`created_at`);

--
-- Indexes for table `trans_log`
--
ALTER TABLE `trans_log`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `account`
--
ALTER TABLE `account`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4624;

--
-- AUTO_INCREMENT for table `adminpanel`
--
ALTER TABLE `adminpanel`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `event`
--
ALTER TABLE `event`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `giftcode`
--
ALTER TABLE `giftcode`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=215;

--
-- AUTO_INCREMENT for table `history_transaction`
--
ALTER TABLE `history_transaction`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3432;

--
-- AUTO_INCREMENT for table `mbbank_log`
--
ALTER TABLE `mbbank_log`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `messages`
--
ALTER TABLE `messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- AUTO_INCREMENT for table `napthe`
--
ALTER TABLE `napthe`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=365;

--
-- AUTO_INCREMENT for table `naptien`
--
ALTER TABLE `naptien`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=120;

--
-- AUTO_INCREMENT for table `payments`
--
ALTER TABLE `payments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1086;

--
-- AUTO_INCREMENT for table `player`
--
ALTER TABLE `player`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3941;

--
-- AUTO_INCREMENT for table `posts`
--
ALTER TABLE `posts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT for table `recharge`
--
ALTER TABLE `recharge`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `sell_item`
--
ALTER TABLE `sell_item`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `setting`
--
ALTER TABLE `setting`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `super_rank`
--
ALTER TABLE `super_rank`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3925;

--
-- AUTO_INCREMENT for table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=763;

--
-- AUTO_INCREMENT for table `trans_log`
--
ALTER TABLE `trans_log`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=38;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `player`
--
ALTER TABLE `player`
  ADD CONSTRAINT `player_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
