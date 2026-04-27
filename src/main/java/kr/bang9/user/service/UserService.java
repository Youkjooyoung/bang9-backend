package kr.bang9.user.service;

import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.user.dao.UserDao;
import kr.bang9.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long createUser(User user) {
        if (userDao.existsByEmail(user.getEmail())) {
            throw new CustomException(ErrorCode.ERR_EMAIL_DUPLICATE);
        }
        userDao.insertUser(user);
        return user.getUserId();
    }

    public User getUser(Long userId) {
        return userDao.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
    }

    public User getUserByEmail(String email) {
        return userDao.findByEmail(email)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_AUTH_INVALID));
    }

    public Optional<User> findByEmail(String email) {
        return userDao.findByEmail(email);
    }

    public Optional<User> findByNameAndPhone(String name, String phone) {
        return userDao.findByNameAndPhone(name, phone);
    }

    public Optional<User> findByEmailAndPhone(String email, String phone) {
        return userDao.findByEmailAndPhone(email, phone);
    }

    public boolean existsByEmail(String email) {
        return userDao.existsByEmail(email);
    }

    @Transactional
    public void updateProfile(User user) {
        userDao.updateProfile(user);
    }

    @Transactional
    public User updateProfile(Long userId, String nickname, String profileImageUrl,
                              String phone, String name, LocalDate birthDate, String gender) {
        User existing = getUser(userId);
        existing.setNickname(nickname);
        existing.setProfileImageUrl(profileImageUrl);
        existing.setPhone(phone);
        existing.setName(name);
        existing.setBirthDate(birthDate);
        existing.setGender(gender);
        userDao.updateProfile(existing);
        return existing;
    }

    @Transactional
    public void updatePassword(Long userId, String passwordHash) {
        userDao.updatePassword(userId, passwordHash);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (user.getPasswordHash() == null
            || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CustomException(ErrorCode.ERR_LOGIN_INVALID);
        }
        userDao.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    @Transactional
    public String resetPassword(Long userId) {
        String temp = generateTempPassword();
        userDao.updatePassword(userId, passwordEncoder.encode(temp));
        return temp;
    }

    private String generateTempPassword() {
        String letters = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
        String digits = "23456789";
        String specials = "@$!%*#?&";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        sb.append(letters.charAt(random.nextInt(letters.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(specials.charAt(random.nextInt(specials.length())));
        String pool = letters + digits + specials;
        for (int i = 0; i < 9; i++) {
            sb.append(pool.charAt(random.nextInt(pool.length())));
        }
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return new String(arr);
    }

    @Transactional
    public void updateLastLoginAt(Long userId) {
        userDao.updateLastLoginAt(userId, LocalDateTime.now());
    }

    @Transactional
    public void withdraw(Long userId) {
        userDao.softDelete(userId, LocalDateTime.now());
    }
}
