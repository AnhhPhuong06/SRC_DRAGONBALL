-- Run in the game's DATA database. Re-running preserves all unrelated item templates.
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO item_template
    (id,type,gender,name,description,level,icon_id,part,is_up_to_up,power_require,gold,gem,head,body,leg)
VALUES
    (1477,75,3,'Hộp SKH Thiên Sứ','Vật phẩm sự kiện',0,15921,0,1,0,0,0,-1,-1,-1),
    (1478,75,3,'Hộp SKH Thiên Sứ Random','Vật phẩm sự kiện',0,15921,0,1,0,0,0,-1,-1,-1)
ON DUPLICATE KEY UPDATE
    type=VALUES(type),gender=VALUES(gender),name=VALUES(name),description=VALUES(description),
    level=VALUES(level),icon_id=VALUES(icon_id),part=VALUES(part),is_up_to_up=VALUES(is_up_to_up),
    power_require=VALUES(power_require),gold=VALUES(gold),gem=VALUES(gem),head=VALUES(head),body=VALUES(body),leg=VALUES(leg);
COMMIT;
