package com.macro.mall.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.macro.mall.dao.OmsOrderDao;
import com.macro.mall.dao.OmsOrderOperateHistoryDao;
import com.macro.mall.dto.*;
import com.macro.mall.mapper.OmsOrderMapper;
import com.macro.mall.mapper.OmsOrderOperateHistoryMapper;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderExample;
import com.macro.mall.model.OmsOrderOperateHistory;
import com.macro.mall.service.OmsOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 订单管理Service实现类
 * Created by macro on 2018/10/11.
 */
@Service
@Slf4j
public class OmsOrderServiceImpl implements OmsOrderService {
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private OmsOrderDao orderDao;
    @Autowired
    private OmsOrderOperateHistoryDao orderOperateHistoryDao;
    @Autowired
    private OmsOrderOperateHistoryMapper orderOperateHistoryMapper;

    @Override
    public List<OmsOrder> list(OmsOrderQueryParam queryParam, Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum, pageSize);
        return orderDao.getList(queryParam);
    }

    @Override
    public JSONObject listCount(OmsOrderQueryParam queryParam) {
        JSONObject object = new JSONObject();
        object.put("week",this.getWeekOrdersCount(queryParam));
        object.put("mouth",this.getWeekOrdersCount(queryParam));
        return object;
    }

    public Integer getWeekOrdersCount(OmsOrderQueryParam queryParam){
        LocalDate now = LocalDate.parse(queryParam.getCreateTime());
        LocalDate early =now.minusDays(7);
        queryParam.setEarlyTime(early.toString());
        List<OmsOrder> list = orderDao.getWeekList(queryParam);
        log.info("一周订单 {}",list.size());
        return list.size();
    }

    public Integer getMouthOrdersCount(OmsOrderQueryParam queryParam){
        LocalDate now = LocalDate.parse(queryParam.getCreateTime());
        LocalDate early =now.minusDays(30);
        queryParam.setEarlyTime(early.toString());
        List<OmsOrder> list = orderDao.getWeekList(queryParam);
        log.info("一个月订单 {}",list.size());
        return list.size();
    }

    @Override
    public JSONObject getTodaySalesInfo(OmsOrderQueryParam queryParam) {
        JSONObject object = new JSONObject();
        object.put("weeks",this.getDuringSales(queryParam,7L));
        object.put("mouth",this.getDuringSales(queryParam,30L));
        object.put("today",this.getTodaySales(queryParam));
        object.put("yesterday",this.getYesterdaySales(queryParam));
        queryParam.setCreateTime(null);
        queryParam.setStatus(0);
        object.put("unPay",this.getOderCount(queryParam));
        queryParam.setStatus(1);
        object.put("unSend",this.getOderCount(queryParam));
        queryParam.setStatus(2);
        object.put("send",this.getOderCount(queryParam));
        queryParam.setStatus(3);
        object.put("finish",this.getOderCount(queryParam));
        queryParam.setStatus(null);
        queryParam.setConfirmStatus(0);
        object.put("confirmStatus",this.getOderCount(queryParam));
        return object;
    }

    private  Integer getOderCount(OmsOrderQueryParam queryParam) {
        log.info("获取不同订单状态");
        List<OmsOrder> list = orderDao.getList(queryParam);
        return list.size();
    }

    /**
     * 一周销售额
     */
    public BigDecimal getDuringSales(OmsOrderQueryParam queryParam,Long days) {
        LocalDate now = LocalDate.parse(queryParam.getCreateTime());
        LocalDate early =now.minusDays(days);
        queryParam.setEarlyTime(early.toString());
        List<OmsOrder> list = orderDao.getWeekList(queryParam);
        final BigDecimal[] salesCount = {new BigDecimal(0)};
        list.stream().forEach(omsOrder -> {
            salesCount[0] = salesCount[0].add(omsOrder.getTotalAmount());
        });
        return salesCount[0];
    }

    /**
     * 当天销售额
     */
    public BigDecimal getTodaySales(OmsOrderQueryParam queryParam) {
        List<OmsOrder> list = orderDao.getList(queryParam);
        final BigDecimal[] salesCount = {new BigDecimal(0)};
        list.stream().forEach(omsOrder -> {
            salesCount[0] = salesCount[0].add(omsOrder.getTotalAmount());
        });
        return salesCount[0];
    }

    /**
     * 昨日销售额
     */
    public BigDecimal getYesterdaySales(OmsOrderQueryParam queryParam) {
        LocalDate now = LocalDate.parse(queryParam.getCreateTime());
        LocalDate yesterDay =now.minusDays(1L);
        queryParam.setCreateTime(yesterDay.toString());
        List<OmsOrder> list = orderDao.getList(queryParam);
        final BigDecimal[] salesCount = {new BigDecimal(0)};
        list.stream().forEach(omsOrder -> {
            salesCount[0] = salesCount[0].add(omsOrder.getTotalAmount());
        });
        return salesCount[0];
    }


    @Override
    public int delivery(List<OmsOrderDeliveryParam> deliveryParamList) {
        //批量发货
        int count = orderDao.delivery(deliveryParamList);
        //添加操作记录
        List<OmsOrderOperateHistory> operateHistoryList = deliveryParamList.stream()
                .map(omsOrderDeliveryParam -> {
                    OmsOrderOperateHistory history = new OmsOrderOperateHistory();
                    history.setOrderId(omsOrderDeliveryParam.getOrderId());
                    history.setCreateTime(new Date());
                    history.setOperateMan("后台管理员");
                    history.setOrderStatus(2);
                    history.setNote("完成发货");
                    return history;
                }).collect(Collectors.toList());
        orderOperateHistoryDao.insertList(operateHistoryList);
        return count;
    }

    @Override
    public int close(List<Long> ids, String note) {
        OmsOrder record = new OmsOrder();
        record.setStatus(4);
        OmsOrderExample example = new OmsOrderExample();
        example.createCriteria().andDeleteStatusEqualTo(0).andIdIn(ids);
        int count = orderMapper.updateByExampleSelective(record, example);
        List<OmsOrderOperateHistory> historyList = ids.stream().map(orderId -> {
            OmsOrderOperateHistory history = new OmsOrderOperateHistory();
            history.setOrderId(orderId);
            history.setCreateTime(new Date());
            history.setOperateMan("后台管理员");
            history.setOrderStatus(4);
            history.setNote("订单关闭:"+note);
            return history;
        }).collect(Collectors.toList());
        orderOperateHistoryDao.insertList(historyList);
        return count;
    }

    @Override
    public int delete(List<Long> ids) {
        OmsOrder record = new OmsOrder();
        record.setDeleteStatus(1);
        OmsOrderExample example = new OmsOrderExample();
        example.createCriteria().andDeleteStatusEqualTo(0).andIdIn(ids);
        return orderMapper.updateByExampleSelective(record, example);
    }

    @Override
    public OmsOrderDetail detail(Long id) {
        return orderDao.getDetail(id);
    }

    @Override
    public int updateReceiverInfo(OmsReceiverInfoParam receiverInfoParam) {
        OmsOrder order = new OmsOrder();
        order.setId(receiverInfoParam.getOrderId());
        order.setReceiverName(receiverInfoParam.getReceiverName());
        order.setReceiverPhone(receiverInfoParam.getReceiverPhone());
        order.setReceiverPostCode(receiverInfoParam.getReceiverPostCode());
        order.setReceiverDetailAddress(receiverInfoParam.getReceiverDetailAddress());
        order.setReceiverProvince(receiverInfoParam.getReceiverProvince());
        order.setReceiverCity(receiverInfoParam.getReceiverCity());
        order.setReceiverRegion(receiverInfoParam.getReceiverRegion());
        order.setModifyTime(new Date());
        int count = orderMapper.updateByPrimaryKeySelective(order);
        //插入操作记录
        OmsOrderOperateHistory history = new OmsOrderOperateHistory();
        history.setOrderId(receiverInfoParam.getOrderId());
        history.setCreateTime(new Date());
        history.setOperateMan("后台管理员");
        history.setOrderStatus(receiverInfoParam.getStatus());
        history.setNote("修改收货人信息");
        orderOperateHistoryMapper.insert(history);
        return count;
    }

    @Override
    public int updateMoneyInfo(OmsMoneyInfoParam moneyInfoParam) {
        OmsOrder order = new OmsOrder();
        order.setId(moneyInfoParam.getOrderId());
        order.setFreightAmount(moneyInfoParam.getFreightAmount());
        order.setDiscountAmount(moneyInfoParam.getDiscountAmount());
        order.setModifyTime(new Date());
        int count = orderMapper.updateByPrimaryKeySelective(order);
        //插入操作记录
        OmsOrderOperateHistory history = new OmsOrderOperateHistory();
        history.setOrderId(moneyInfoParam.getOrderId());
        history.setCreateTime(new Date());
        history.setOperateMan("后台管理员");
        history.setOrderStatus(moneyInfoParam.getStatus());
        history.setNote("修改费用信息");
        orderOperateHistoryMapper.insert(history);
        return count;
    }

    @Override
    public int updateNote(Long id, String note, Integer status) {
        OmsOrder order = new OmsOrder();
        order.setId(id);
        order.setNote(note);
        order.setModifyTime(new Date());
        int count = orderMapper.updateByPrimaryKeySelective(order);
        OmsOrderOperateHistory history = new OmsOrderOperateHistory();
        history.setOrderId(id);
        history.setCreateTime(new Date());
        history.setOperateMan("后台管理员");
        history.setOrderStatus(status);
        history.setNote("修改备注信息："+note);
        orderOperateHistoryMapper.insert(history);
        return count;
    }

}
