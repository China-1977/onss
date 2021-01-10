package work.onss.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import work.onss.domain.Cart;
import work.onss.domain.Product;
import work.onss.domain.Store;
import work.onss.vo.Work;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class CartController {

    @Autowired
    protected MongoTemplate mongoTemplate;

    /**
     * @param uid 用户ID
     * @param id  主键
     * @return 删除购物车商品
     */
    @DeleteMapping(value = {"carts/{id}"})
    public Work<Boolean> delete(@PathVariable String id, @RequestParam(name = "uid") String uid) {
        Query query = Query.query(Criteria.where("id").is(id).and("uid").is(uid));
        mongoTemplate.remove(query, Cart.class);
        return Work.success("删除成功", true);
    }

    /**
     * @param uid  用户ID
     * @param cart 购物车
     * @return 更新购车商品数量
     */
    @PostMapping(value = {"carts"})
    public Work<Cart> updateNum(@RequestParam(name = "uid") String uid, @Validated @RequestBody Cart cart) {
        Query queryProduct = Query.query(Criteria.where("id").is(cart.getPid()));
        Product product = mongoTemplate.findOne(queryProduct, Product.class);
        if (product != null) {
            if (cart.getNum().compareTo(product.getMax()) > 0 || cart.getNum().compareTo(product.getMin()) < 0) {
                String message = MessageFormat.format("每次仅限购买{0}至{1}", product.getMin(), product.getMax());
                return Work.fail(message);
            } else if (cart.getNum().compareTo(product.getStock()) > 0) {
                return Work.fail("库存不足");
            } else if (!product.getStatus()) {
                return Work.fail("该商品已下架");
            }
            Query cartQuery = Query.query(Criteria.where("pid").is(cart.getPid()).and("uid").is(uid));
            if (null != cart.getId()) {
                cartQuery.addCriteria(Criteria.where("id").is(cart.getId()));
            }
            BigDecimal total = product.getAverage().multiply(cart.getNum());
            Cart oldCart = mongoTemplate.findOne(cartQuery, Cart.class);
            if (oldCart != null) {
                oldCart.setNum(cart.getNum());
                oldCart.setTotal(total);
                Update updateCart = Update.update("num", oldCart.getNum()).set("total", total);
                mongoTemplate.updateFirst(cartQuery, updateCart, Cart.class);
                return Work.success("更新购物车成功", oldCart);
            } else {
                cart.setUid(uid);
                cart.setTotal(total);
                mongoTemplate.insert(cart);
                return Work.success("加入购物车成功", cart);
            }
        } else {
            return Work.fail("该商品已下架");
        }
    }

    /**
     * @param uid 用户ID
     * @return 购物车商户
     */
    @GetMapping(value = {"carts/getStores"})
    public Work<List<Store>> getStores(@RequestParam(name = "uid") String uid) {
        Query query = Query.query(Criteria.where("uid").is(uid));
        List<String> sids = mongoTemplate.findDistinct(query, "sid", Cart.class, String.class);
        if (sids.size() == 0) {
            return Work.success("加载成功", null);
        } else {
            Query storeQuery = Query.query(Criteria.where("id").in(sids));
            storeQuery.fields()
                    .exclude("customers")
                    .exclude("products")
                    .exclude("merchant");
            List<Store> stores = mongoTemplate.find(storeQuery, Store.class);
            return Work.success("加载成功", stores);
        }
    }

    /**
     * @param sid 商户ID
     * @return 购物车
     */
    @GetMapping(value = {"carts"})
    public Work<List<Product>> getCarts(@RequestParam(name = "sid") String sid, @RequestParam(name = "uid") String uid) {
        Query cartQuery = Query.query(Criteria.where("uid").is(uid).and("sid").is(sid));
        List<Cart> carts = mongoTemplate.find(cartQuery, Cart.class);
        Map<String, Cart> cartsPid = carts.stream().collect(Collectors.toMap(Cart::getPid, cart -> cart));
        Query productQuery = Query.query(Criteria.where("id").in(cartsPid.keySet()).and("sid").is(sid));
        List<Product> products = mongoTemplate.find(productQuery, Product.class);
        return Work.success("加载成功", products);
    }
}
