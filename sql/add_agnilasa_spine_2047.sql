-- Item ID 2047; client Spine appearance ID 9000.
-- Leaves an existing row at this item ID untouched.
-- Requires the matching Agnilasa Spine assets in the Java ME client.
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO item_template
    (id,type,gender,name,description,level,icon_id,part,is_up_to_up,power_require,gold,gem,head,body,leg)
SELECT
    2047,5,3,'Agnilasa <spine=9000>','Cải trang Agnilasa. Cần client có tài nguyên Spine Agnilasa.',
    0,2127,9000,0,0,0,0,-1,-1,9000
WHERE NOT EXISTS (SELECT 1 FROM item_template WHERE id=2047);
COMMIT;
