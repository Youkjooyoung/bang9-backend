package kr.bang9.admin.dao;

import java.util.List;
import java.util.Optional;
import kr.bang9.admin.domain.AdminLog;
import kr.bang9.admin.dto.AdminListingView;
import kr.bang9.admin.dto.AdminLogView;
import kr.bang9.admin.dto.AdminOrderView;
import kr.bang9.admin.dto.AdminReportView;
import kr.bang9.admin.dto.AdminUserView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminDao {

    int insertLog(AdminLog log);

    List<AdminLogView> findLogs(@Param("offset") int offset, @Param("size") int size);

    int countLogs();

    List<AdminUserView> findUsers(@Param("keyword") String keyword,
                                  @Param("status") String status,
                                  @Param("role") String role,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    int countUsers(@Param("keyword") String keyword,
                   @Param("status") String status,
                   @Param("role") String role);

    Optional<AdminUserView> findUserById(@Param("userId") Long userId);

    int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    int updateUserRole(@Param("userId") Long userId, @Param("role") String role);

    List<AdminListingView> findListings(@Param("status") String status,
                                        @Param("keyword") String keyword,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    int countListings(@Param("status") String status, @Param("keyword") String keyword);

    int updateListingStatus(@Param("listingId") Long listingId,
                            @Param("status") String status,
                            @Param("rejectReason") String rejectReason);

    List<AdminReportView> findReports(@Param("status") String status,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    int countReports(@Param("status") String status);

    Optional<AdminReportView> findReportById(@Param("reportId") Long reportId);

    int updateReportStatus(@Param("reportId") Long reportId, @Param("status") String status);

    List<AdminOrderView> findOrders(@Param("status") String status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    int countOrders(@Param("status") String status, @Param("keyword") String keyword);
}
