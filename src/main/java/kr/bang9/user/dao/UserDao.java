package kr.bang9.user.dao;

import kr.bang9.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface UserDao {

    void insertUser(User user);

    Optional<User> findById(@Param("userId") Long userId);

    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findByOauth(@Param("provider") String provider, @Param("providerId") String providerId);

    Optional<User> findByNameAndPhone(@Param("name") String name, @Param("phone") String phone);

    Optional<User> findByEmailAndPhone(@Param("email") String email, @Param("phone") String phone);

    boolean existsByEmail(@Param("email") String email);

    void updateLastLoginAt(@Param("userId") Long userId, @Param("loginAt") LocalDateTime loginAt);

    void updateProfile(User user);

    void updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    void linkOauth(@Param("userId") Long userId, @Param("provider") String provider, @Param("providerId") String providerId);

    void unlinkOauth(@Param("userId") Long userId);

    void softDelete(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);
}
