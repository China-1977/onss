import { prefix, wxLogin, windowWidth, domain, getProduct, wxRequest } from '../../utils/util.js';
Page({
  data: {
    windowWidth, domain, prefix
  },
  onLoad: function (options) {
    const authorization = wx.getStorageSync('authorization');
    const user = wx.getStorageSync('user');
    getProduct(options.id, authorization, user.id).then((data) => {
      console.log(data.content);
      this.setData({
        ...data.content
      });
    })
  },

  switch2Change: function (e) {
    wxLogin().then(({ user, authorization }) => {
      const { id, isLike } = this.data;
      if (e.detail.value) {
        wxRequest({
          url: `${domain}/prefers?uid=${user.id}`,
          method: 'POST',
          header: { authorization },
        }).then((data) => {
          this.setData({
            isLike: id
          });
        });
      } else {
        wxRequest({
          url: `${domain}/prefers/${isLike}?uid=${user.id}`,
          method: 'DELETE',
          header: { authorization },
        }).then(() => {
          this.setData({
            isLike: null
          });
        });
      }
    })
  },


  addCount: function () {
    this.updateCart(1);
  },

  subtractCount: function () {
    this.updateCart(-1);
  },

  updateCart: function (count) {
    wxLogin().then(({ user, authorization }) => {
      const { sid, id, cartsPid } = this.data;
      if (cartsPid[id]) {
        wxRequest({
          url: `${domain}/carts?uid=${user.id}`,
          method: 'POST',
          header: { authorization },
          data: { id: cartsPid[id].id, sid: sid, pid: id, num: cartsPid[id].num + count },
        }).then((data) => {
          const cartsPid = { ...this.data.cartsPid, [id]: data.content };
          wx.setStorageSync('cartsPid', cartsPid);
          this.setData({
            cartsPid
          });
        });
      } else {
        wxRequest({
          url: `${domain}/carts?uid=${user.id}`,
          method: 'POST',
          header: { authorization },
          data: { sid: sid, pid: id, num: 1 },
        }).then((data) => {
          const cartsPid = { ...this.data.cartsPid, [id]: data.content };
          wx.setStorageSync('cartsPid', cartsPid);
          this.setData({
            cartsPid
          });
        });
      }
    })
  }
})