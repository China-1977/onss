package work.onss.controller;

import com.ijpay.core.enums.SignType;
import com.ijpay.core.kit.IpKit;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.model.UnifiedOrderModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import work.onss.config.WeChatConfig;
import work.onss.domain.Cart;
import work.onss.domain.Product;
import work.onss.domain.Score;
import work.onss.domain.Store;
import work.onss.vo.Work;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
@RestController
public class ScoreController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private WeChatConfig weChatConfig;

    /**
     * @param id  主键
     * @param uid 用户ID
     * @return 订单信息
     */
    @GetMapping(value = {"scores/{id}"})
    public Work<Score> score(@PathVariable String id, @RequestParam(name = "uid") String uid) {
        Query query = Query.query(Criteria.where("id").is(id).and("uid").is(uid));
        Score score = mongoTemplate.findOne(query, Score.class);
        return Work.success("加载成功", score);
    }

    /**
     * @param uid      用户ID
     * @param pageable 默认创建时间排序并分页
     * @return 订单分页
     */
    @GetMapping(value = {"scores"})
    public Work<Page<Score>> all(@RequestParam(name = "uid") String uid,
                                 @PageableDefault(sort = {"insertTime", "updateTime"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Query query = Query.query(Criteria.where("uid").is(uid)).with(pageable);
        List<Score> scores = mongoTemplate.find(query, Score.class);
        Page<Score> page = new PageImpl<>(scores);
        return Work.success("加载成功", page);
    }

    /**
     * @param uid   用户ID
     * @param score 订单信息
     * @return 订单信息
     */
    @PostMapping(value = {"scores"})
    public Work<Map<String, String>> score(@RequestParam(name = "uid") String uid, @RequestBody Score score, HttpServletRequest request) {
        Store store = mongoTemplate.findById(score.getSid(), Store.class);
        if (store == null) {
            return Work.fail("该店铺不存,请联系客服!");
        }
        if (!store.getStatus()) {
            String message = MessageFormat.format("营业时间:{0}-{1}", store.getOpenTime(), store.getCloseTime());
            return Work.fail(message);
        }
        Map<String, Cart> carts = score.getCarts();
        Set<String> pids = carts.keySet();
        Query query = Query.query(Criteria.where("id").in(pids).and("sid").is(score.getSid()));
        List<Product> products = mongoTemplate.find(query, Product.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Product product : products) {
            Cart cart = carts.get(product.getId());
            if (cart.getNum() > product.getStock()) {
                String message = MessageFormat.format("[{0}]库存不足!", product.getName());
                return Work.fail(message);
            } else if (cart.getNum() > product.getMax()) {
                String message = MessageFormat.format("[{0}]每次限购{1}", product.getName(), product.getMax());
                return Work.fail(message);
            }
            product.setNum(cart.getNum());
            product.setTotal(product.getAverage().multiply(BigDecimal.valueOf(cart.getNum())));
            total = total.add(product.getTotal());
        }
        if (total.equals(BigDecimal.ZERO) && 0 == products.size()) {
            return Work.fail("请选择购买的商品!");
        }
        score.setUid(uid);
        score.setProducts(products);
        score.setTotal(total);

        /*
          服务商APPID
          服务商商户号
          当前调起支付的小程序APPID
          微信支付分配的子商户号
          随机字符串，不长于32位
          商品描述
          商户系统内部订单号，要求32个字符内，只能是数字、大小写字母_-|*且在同一个商户号下唯一
          订单总金额，只能为整数 单位为分
          支持IPV4和IPV6两种格式的IP地址。调用微信支付API的机器IP
          通知地址
          小程序取值如下：JSAPI
          trade_type=JSAPI，此参数必传，用户在子商户appid下的唯一标识。openid和sub_openid可以选传其中之一，如果选择传sub_openid,则必须传sub_appid
         */
        Map<String, String> sign = UnifiedOrderModel.builder()
                .appid(weChatConfig.getAppId())
                .mch_id(weChatConfig.getMchId())
                .sub_appid(score.getSubAppId())
                .sub_mch_id(store.getSubMchId())
                .nonce_str(WxPayKit.generateStr())
                .body(store.getName())
                .out_trade_no(WxPayKit.generateStr())
                .total_fee(String.valueOf(score.getTotal().movePointRight(2).intValue()))
                .spbill_create_ip(IpKit.getRealIp(request))
                .notify_url(weChatConfig.getNotifyUrl())
                .trade_type("JSAPI")
                .sub_openid(score.getOpenid())
                .build()
                .createSign(weChatConfig.getApiKey(), SignType.HMACSHA256);

        String xmlResult = WxPayApi.pushOrder(true, sign);
        Map<String, String> resultMap = WxPayKit.xmlToMap(xmlResult);

        String returnCode = resultMap.get("return_code");
        String returnMsg = resultMap.get("return_msg");
        if (!WxPayKit.codeIsOk(returnCode)) {
            return Work.fail(returnMsg);
        }
        String resultCode = resultMap.get("result_code");
        if (!WxPayKit.codeIsOk(resultCode)) {
            return Work.fail(returnMsg);
        }
        /*
            timeStamp	String	是	时间戳从1970年1月1日00:00:00至今的秒数,即当前的时间
            nonceStr	String	是	随机字符串，长度为32个字符以下。
            package	String	是	统一下单接口返回的 prepay_id 参数值，提交格式如：prepay_id=*
            signType	String	是	签名算法，暂支持 MD5
            paySign	String	是	签名,具体签名方案参见微信公众号支付帮助文档;
            success	Function	否	接口调用成功的回调函数
            fail	Function	否	接口调用失败的回调函数
            complete	Function	否	接口调用结束的回调函数（调用成功、失败都会执行）
        */

        // 以下字段在return_code 和result_code都为SUCCESS的时候有返回
        String prepayId = resultMap.get("prepay_id");
        // 二次签名，构建公众号唤起支付的参数,这里的签名方式要与上面统一下单请求签名方式保持一致
        Map<String, String> packageParams = WxPayKit.miniAppPrepayIdCreateSign(store.getSubMchId(), prepayId, weChatConfig.getApiKey(), SignType.HMACSHA256);
        score.setPrepayId(prepayId);
        mongoTemplate.insert(score);

        return Work.success("创建订单成功", packageParams);
    }
}
