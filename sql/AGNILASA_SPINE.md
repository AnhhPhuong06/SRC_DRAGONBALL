# Agnilasa Spine

The local DATA database now contains item 2047, type 5, gender 3, part 9000,
leg 9000, named `Agnilasa <spine=9000>`. Icon 2127 is an existing placeholder.
The migration leaves every other item untouched and does not grant inventory items.

`src/data/DataGame.java` now advertises item version 18 so clients refresh templates.
`src/server/Manager.java` loads templates ordered by ID, matching this protocol's
implicit item indices. The current server process has not been restarted.

The requested Spine model is still missing. The Java ME client needs its skeleton,
atlas and all atlas PNG pages under:

`resources/SpineData/caitrang/9000-Agnilasa_dragon_ball1/`

Once those files are supplied and the client is rebuilt, start the rebuilt server,
log in again with an authorized admin account and use `i 2047 1`.
The item record and mapping alone do not provide an animated Agnilasa model.
