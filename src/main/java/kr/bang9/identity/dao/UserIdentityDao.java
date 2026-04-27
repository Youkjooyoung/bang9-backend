package kr.bang9.identity.dao;

import kr.bang9.identity.domain.UserIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserIdentityDao {

    UserIdentity findByUserId(@Param("userId") Long userId);

    UserIdentity findByRrnHash(@Param("rrnHash") String rrnHash);

    int insert(UserIdentity identity);
}
