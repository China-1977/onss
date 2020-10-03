import { prefix, windowWidth, checkStore, domain, wxRequest, types, chooseImages,chooseImage } from '../../utils/util.js';
Page({
  data: {
    prefix, domain, windowWidth, types,videos:[]
  },
  bindPickerChange: function (e) {
    const id = e.currentTarget.id;
    const index = e.currentTarget.dataset.index;
    const range = e.currentTarget.dataset.range;
    const ranges = this.data[range];
    const value = e.detail.value;
    this.setData({
      [id]: ranges[value].id,
      [index]: value
    })
  },
  getLocation: function (e) {
    const y = e.currentTarget.dataset.y;
    const x = e.currentTarget.dataset.x;
    const name = e.currentTarget.dataset.name;
    if (x && y) {
      wx.chooseLocation({
        longitude: x,
        latitude: y,
        name: name,
        success: (res) => {
          this.setData({ location: { x: res.longitude, y: res.latitude, coordinates: [res.longitude, res.latitude] } })
        }
      })
    } else {
      wx.getLocation({
        type: 'gcj02',
        success: (res) => {
          wx.chooseLocation({
            longitude: parseFloat(res.longitude),
            latitude: parseFloat(res.latitude),
            success: (res) => {
              this.setData({ location: { x: res.longitude, y: res.latitude, coordinates: [res.longitude, res.latitude] } })
            }
          })
        }
      })
    }

  },

  chooseImages: function (e) {
    const id = e.currentTarget.id;
    let count = e.currentTarget.dataset.count
    const length = this.data[id].length;
    count = count - length;
    checkStore().then(({ authorization, customer }) => {
      chooseImages(authorization, count, `${domain}/stores/${customer.store.id}/uploadPicture`).then((data) => {
        const pictures = this.data[id];
        if (!pictures.includes(data)) {
          this.setData({
            [`${id}[${length}]`]: data
          })
        }
      })
    })
  },

  chooseImage: function (e) {
    const id = e.currentTarget.id;
    checkStore().then(({ authorization, customer }) => {
      chooseImage(authorization, `${domain}/stores/${customer.store.id}/uploadPicture`).then((data) => {
        this.setData({
          [`${id}`]: data
        })
      })
    })
  },

  deletePictures: function (e) {
    const id = e.currentTarget.id;
    const index = e.currentTarget.dataset.index;
    const files = this.data[id];
    files.splice(index, 1);
    this.setData({
      [id]: files
    })
  },

  deletePicture: function (e) {
    const id = e.currentTarget.id;
    this.setData({
      [id]: null
    })
  },

  clearPictues: function (e) {
    const id = e.currentTarget.id;
    this.setData({
      [id]: []
    })
  },
  videosBindInput: function (e) {
    console.log(e);
    this.setData({
      videos: e.detail.value.split(",")
    })
  },

  updateStore: function (e) {
    console.log(e.detail.value)
    const store = {
      ...e.detail.value,
      location: this.data.location,
      pictures: this.data.pictures,
      videos: this.data.videos
    }
    const customer = wx.getStorageSync('customer');
    const authorization = wx.getStorageSync('authorization');
    wx.request({
      url: `${domain}/stores/${customer.store.id}?cid=${customer.id}`,
      data: store,
      method: "PUT",
      header: {
        authorization,
      },
      success: ({ data }) => {
        const { code, msg, content } = data;
        console.log(data)
        switch (code) {
          case 'success':
            wx.showToast({
              title: msg,
              icon: 'success',
              duration: 2000,
              success: (res) => {
                const store = { ...this.data.store, ...content }
                this.setData({
                  store
                })
                let pages = getCurrentPages();//当前页面栈
                let detail = pages[pages.length - 2];//详情页面
                detail.setData({
                  ...store
                });
              }
            })
            break;
          case 'fail.login':
            wx.redirectTo({
              url: '/pages/login/login',
            })
            break;
          default:
            wx.showModal({
              title: '警告',
              content: msg,
              confirmColor: '#e64340',
              showCancel: false,
            })
            break;
        }
      },
      fail: (res) => {
        wx.showModal({
          title: '警告',
          content: '加载失败',
          confirmColor: '#e64340',
          showCancel: false,
        })
      },
    })
  },

  onLoad: function (options) {
    checkStore().then(({ authorization, customer }) => {
      wxRequest({
        url: `${domain}/stores/${customer.store.id}?cid=${customer.id}`,
        header: { authorization },
      }).then((store) => {
        let index = -1;
        types.find((value, key, array) => {
          if (value.id === store.type) {
            index = key;
          }
        })
        this.setData({
          ...store, index, store
        })
      })
    })
  },

  resetForm: function (e) {
    const store = this.data.store;
    let index = this.data.index;
    types.find((value, key, array) => {
      if (value.id === store.type) {
        index = key;
      }
    })
    this.setData({
      ...store, index
    })
  },
})