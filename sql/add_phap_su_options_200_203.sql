-- Chọn database game trước khi chạy. Có thể chạy lại an toàn.
INSERT INTO `item_option_template` (`id`, `name`) VALUES
(200, 'Trang Bị Pháp Sư # lần'),
(201, 'SĐ Pháp Sư #%'),
(202, 'HP Pháp Sư #%'),
(203, 'KI Pháp Sư #%')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
