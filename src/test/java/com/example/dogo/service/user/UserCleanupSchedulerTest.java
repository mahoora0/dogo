package com.example.dogo.service.user;

import com.example.dogo.entity.user.User;
import com.example.dogo.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCleanupSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserHardDeleteService userHardDeleteService;

    @Test
    void continuesWithNextUserWhenOneDeletionFails() {
        User first = user(1L, "first");
        User second = user(2L, "second");
        when(userRepository.findByStatusAndWithdrawnAtBefore(org.mockito.ArgumentMatchers.eq("WITHDRAWN"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("blocked")).when(userHardDeleteService).deleteUser(1L);

        new UserCleanupScheduler(userRepository, userHardDeleteService).cleanupWithdrawnUsers();

        InOrder inOrder = inOrder(userHardDeleteService);
        inOrder.verify(userHardDeleteService).deleteUser(1L);
        inOrder.verify(userHardDeleteService).deleteUser(2L);
    }

    private User user(Long userNo, String nickname) {
        User user = new User("user" + userNo + "@example.com", nickname, "010-0000-0000");
        ReflectionTestUtils.setField(user, "userNo", userNo);
        return user;
    }
}
