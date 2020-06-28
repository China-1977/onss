package work.onss.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import work.onss.vo.wx.Merchant;
import work.onss.vo.wx.UboInfo;

import java.util.Collection;

@Log4j2
@Data
@NoArgsConstructor
@ToString
@Document
public class MerchantInfo {
    @Id
    private String id;
    //超级管理员
    private String contactName;//姓名
    private String contactIdNumber;//身份证
    private String mobilePhone;//手机号
    private String contactEmail;//常用邮箱

    //营业执照
    private String subjectType;//主体类型
    private String licenseCopy;//五证合一
    private String licenseNumber;//社会信用代码
    private String merchantName;//商户全称
    private String legalPerson;//经营者姓名

    //法人信息
    private String idCardCopy;//身份证正面
    private String idCardNumber;//身份证号
    private String idCardName;//姓名
    private String idCardNational;//身份证反面
    private String cardPeriodBegin;//开始时间
    private String cardPeriodEnd;//结束时间
    private Boolean owner;//是否是最终受益人

    //最终受益人
    private UboInfo uboInfo;

    //经营资料
    private String merchantShortname;//商户简称
    private String servicePhone;//客服电话
    private Collection<String> miniProgramPics;//小程序截图

    //结算规则
    private String settlementId;//入驻结算规则ID
    private String qualificationType;//所属行业
    private Collection<String> qualifications;//特殊资质图片

    //结算银行账户
    private Merchant.BankAccountEnum bankAccountType;//账户类型 个人 公户
    private String accountName;//开户名称
    private Integer accountBank;//开户银行 0-17
    private Address bankAddress;//开户地址
    private String accountNumber;//银行账号
    private String bankName;//其他银河时 银行全称


}