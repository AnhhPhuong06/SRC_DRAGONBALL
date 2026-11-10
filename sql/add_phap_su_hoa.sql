-- Chọn database game trước khi chạy. Có thể chạy lại an toàn.
INSERT INTO `item_template`
(`id`, `TYPE`, `gender`, `NAME`, `description`, `level`, `icon_id`, `part`, `is_up_to_up`, `power_require`, `gold`, `gem`, `head`, `body`, `leg`) VALUES
(1455, 27, 3, 'Đá pháp sư', 'Vật phẩm sự kiện', 0, 21321, 0, 1, 0, 0, 0, -1, -1, -1),
(1456, 27, 3, 'Bùa tẩy pháp sư', 'Vật phẩm sự kiện', 0, 11573, 0, 1, 0, 0, 0, -1, -1, -1)
ON DUPLICATE KEY UPDATE `TYPE`=VALUES(`TYPE`), `gender`=VALUES(`gender`), `NAME`=VALUES(`NAME`),
`description`=VALUES(`description`), `level`=VALUES(`level`), `icon_id`=VALUES(`icon_id`),
`part`=VALUES(`part`), `is_up_to_up`=VALUES(`is_up_to_up`), `power_require`=VALUES(`power_require`),
`gold`=VALUES(`gold`), `gem`=VALUES(`gem`), `head`=VALUES(`head`), `body`=VALUES(`body`), `leg`=VALUES(`leg`);

INSERT INTO `item_option_template` (`id`, `name`) VALUES
(200, 'Trang Bị Pháp Sư # lần'),
(201, 'SĐ Pháp Sư #%'),
(202, 'HP Pháp Sư #%'),
(203, 'KI Pháp Sư #%'),
(204, 'SĐCM Pháp Sư #%')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);
