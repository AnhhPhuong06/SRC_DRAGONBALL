-- Chọn database game trước khi chạy. Có thể chạy lại an toàn.
INSERT INTO `item_option_template` (`id`, `name`) VALUES
(205, 'Siêu hóa thành công #/20 lần')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);
