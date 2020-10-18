import { prefix, wxLogin, wxRequest, domain, size } from '../../utils/util.js';
Page({

  data: {
    prefix, number: 0, last: false, products: []
  },

  onLoad: function (options) {
    wxLogin().then(({ authorization, user, cartsPid }) => {
      wxRequest({
        url: `${domain}/prefers?uid=${user.id}&page=0&size=${size}`,
        header: { authorization },
      }).then((data) => {
        console.log(data);
        this.setData({
          cartsPid,
          number: 0,
          last: false,
          products: data.content
        })
      })
    })
  },


  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {
    wxLogin().then(({ authorization, user }) => {
      wxRequest({
        url: `${domain}/prefers?uid=${user.id}&page=0&size=${size}`,
        header: { authorization },
      }).then((data) => {
        this.setData({
          number: 0,
          last: false,
          products: data.content
        })
        wx.stopPullDownRefresh()
      })
    })
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function () {

    wxLogin().then(({ authorization, user }) => {
      const number = this.data.number + 1;
      wxRequest({
        url: `${domain}/prefers?uid=${user.id}&page=${number}&size=${size}`,
        header: { authorization },
      }).then((data) => {
        if (data.content.length == 0) {
          this.setData({
            last: true,
          });
        } else {
          const products = this.data.products.concat(data.content)
          this.setData({ number, products, });
        }
      })
    })
  },

  addCount: function (e) {
    const id = e.currentTarget.id;
    this.updateCart(id, 1);
  },

  subtractCount: function (e) {
    const id = e.currentTarget.id;
    this.updateCart(id, -1);
  },

  updateCart: function (index, count) {
    wxLogin().then(({ user, authorization }) => {
      let product = this.data.products[index]
      const { cartsPid } = this.data;
      if (cartsPid[product.id]) {
        wxRequest({
          url: `${domain}/carts?uid=${user.id}`,
          method: 'POST',
          header: {
            authorization,
          },
          data: { id: cartsPid[product.id].id, sid: product.sid, pid: product.id, num: cartsPid[product.id].num + count },
        }).then((data) => {
          const cartsPid = { ...this.data.cartsPid, [product.id]: data.content };
          wx.setStorageSync('cartsPid', cartsPid);
          this.setData({
            cartsPid
          });
        });
      } else {
        wxRequest({
          url: `${domain}/carts?uid=${user.id}`,
          method: 'POST',
          header: {
            authorization,
          },
          data: { sid: product.sid, pid: product.id, num: 1 },
        }).then((data) => {
          const cartsPid = { ...this.data.cartsPid, [product.id]: data.content };
          wx.setStorageSync('cartsPid', cartsPid);
          this.setData({
            cartsPid
          });
        });
      }
    })
  }

})