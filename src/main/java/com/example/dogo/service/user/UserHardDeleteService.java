package com.example.dogo.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserHardDeleteService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteUser(Long userNo) {
        jdbcTemplate.update("DELETE FROM USER_SOCIAL_ACCOUNT WHERE USER_NO = ?", userNo);

        jdbcTemplate.update("DELETE FROM POST_REPORT WHERE REPORTER_NO = ?", userNo);
        // Keep report audit records while removing the withdrawn admin reference.
        jdbcTemplate.update("UPDATE POST_REPORT SET HANDLER_NO = NULL WHERE HANDLER_NO = ?", userNo);

        jdbcTemplate.update("""
                DELETE FROM CHAT_MESSAGE
                WHERE SENDER_NO = ?
                   OR ROOM_ID IN (
                       SELECT ROOM_ID
                       FROM CHAT_ROOM
                       WHERE INQUIRER_NO = ?
                          OR OWNER_NO = ?
                          OR LOST_ID IN (SELECT LOST_ID FROM LOST_ITEM WHERE USER_NO = ?)
                          OR FOUND_ID IN (SELECT FOUND_ID FROM FOUND_ITEM WHERE USER_NO = ?)
                   )
                """, userNo, userNo, userNo, userNo, userNo);
        jdbcTemplate.update("""
                DELETE FROM CHAT_ROOM
                WHERE INQUIRER_NO = ?
                   OR OWNER_NO = ?
                   OR LOST_ID IN (SELECT LOST_ID FROM LOST_ITEM WHERE USER_NO = ?)
                   OR FOUND_ID IN (SELECT FOUND_ID FROM FOUND_ITEM WHERE USER_NO = ?)
                """, userNo, userNo, userNo, userNo);

        jdbcTemplate.update("DELETE FROM INQUIRY_FILE WHERE INQUIRY_ID IN (SELECT INQUIRY_ID FROM INQUIRY WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM INQUIRY WHERE USER_NO = ?", userNo);

        jdbcTemplate.update("DELETE FROM ANIMAL_REPORT_MATCH WHERE MISSING_REPORT_ID IN (SELECT REPORT_ID FROM ANIMAL_REPORT WHERE USER_NO = ?) " +
                "OR SIGHTING_REPORT_ID IN (SELECT REPORT_ID FROM ANIMAL_REPORT WHERE USER_NO = ?)", userNo, userNo);
        jdbcTemplate.update("DELETE FROM ANIMAL_REPORT_IMAGE_EMBEDDING WHERE REPORT_ID IN (SELECT REPORT_ID FROM ANIMAL_REPORT WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM ANIMAL_REPORT_IMAGE WHERE REPORT_ID IN (SELECT REPORT_ID FROM ANIMAL_REPORT WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM ANIMAL_REPORT WHERE USER_NO = ?", userNo);

        jdbcTemplate.update("DELETE FROM MISSING_PERSON_REPORT WHERE USER_NO = ?", userNo);

        jdbcTemplate.update("DELETE FROM ITEM_MATCH WHERE LOST_ID IN (SELECT LOST_ID FROM LOST_ITEM WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM ITEM_EMBEDDING WHERE ITEM_TYPE = 'LOST' AND ITEM_ID IN (SELECT LOST_ID FROM LOST_ITEM WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM LOST_ITEM_IMAGE WHERE LOST_ID IN (SELECT LOST_ID FROM LOST_ITEM WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM LOST_ITEM WHERE USER_NO = ?", userNo);

        jdbcTemplate.update("DELETE FROM ITEM_MATCH WHERE FOUND_ID IN (SELECT FOUND_ID FROM FOUND_ITEM WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM ITEM_EMBEDDING WHERE ITEM_TYPE = 'FOUND' AND ITEM_ID IN (SELECT FOUND_ID FROM FOUND_ITEM WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM FOUND_ITEM_IMAGE WHERE FOUND_ID IN (SELECT FOUND_ID FROM FOUND_ITEM WHERE USER_NO = ?)", userNo);
        jdbcTemplate.update("DELETE FROM FOUND_ITEM WHERE USER_NO = ?", userNo);

        jdbcTemplate.update("DELETE FROM USERS WHERE USER_NO = ?", userNo);
    }
}
