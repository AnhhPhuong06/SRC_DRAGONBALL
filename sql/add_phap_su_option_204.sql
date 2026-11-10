-- Chọn database game trước khi chạy. Có thể chạy lại an toàn.
INSERT INTO `item_option_template` (`id`, `name`) VALUES
(204, 'SĐCM Pháp Sư #%')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
