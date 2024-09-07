
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 失物认领
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/shiwuYuyue")
public class ShiwuYuyueController {
    private static final Logger logger = LoggerFactory.getLogger(ShiwuYuyueController.class);

    private static final String TABLE_NAME = "shiwuYuyue";

    @Autowired
    private ShiwuYuyueService shiwuYuyueService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private GonggaoService gonggaoService;//公告信息
    @Autowired
    private ShiwuService shiwuService;//失物招领
    @Autowired
    private XunwuService xunwuService;//寻物启示
    @Autowired
    private XunwuYuyueService xunwuYuyueService;//寻物认领
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = shiwuYuyueService.queryPage(params);

        //字典表数据转换
        List<ShiwuYuyueView> list =(List<ShiwuYuyueView>)page.getList();
        for(ShiwuYuyueView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ShiwuYuyueEntity shiwuYuyue = shiwuYuyueService.selectById(id);
        if(shiwuYuyue !=null){
            //entity转view
            ShiwuYuyueView view = new ShiwuYuyueView();
            BeanUtils.copyProperties( shiwuYuyue , view );//把实体数据重构到view中
            //级联表 失物招领
            //级联表
            ShiwuEntity shiwu = shiwuService.selectById(shiwuYuyue.getShiwuId());
            if(shiwu != null){
            BeanUtils.copyProperties( shiwu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setShiwuId(shiwu.getId());
            }
            //级联表 用户
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(shiwuYuyue.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody ShiwuYuyueEntity shiwuYuyue, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,shiwuYuyue:{}",this.getClass().getName(),shiwuYuyue.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            shiwuYuyue.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<ShiwuYuyueEntity> queryWrapper = new EntityWrapper<ShiwuYuyueEntity>()
            .eq("shiwu_id", shiwuYuyue.getShiwuId())
            .eq("yonghu_id", shiwuYuyue.getYonghuId())
            .in("shiwu_yuyue_yesno_types", new Integer[]{1,2})
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShiwuYuyueEntity shiwuYuyueEntity = shiwuYuyueService.selectOne(queryWrapper);
        if(shiwuYuyueEntity==null){
            shiwuYuyue.setInsertTime(new Date());
            shiwuYuyue.setShiwuYuyueYesnoTypes(1);
            shiwuYuyue.setCreateTime(new Date());
            shiwuYuyueService.insert(shiwuYuyue);
            return R.ok();
        }else {
            if(shiwuYuyueEntity.getShiwuYuyueYesnoTypes()==1)
                return R.error(511,"有相同的待审核的数据");
            else if(shiwuYuyueEntity.getShiwuYuyueYesnoTypes()==2)
                return R.error(511,"有相同的审核通过的数据");
            else
                return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ShiwuYuyueEntity shiwuYuyue, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,shiwuYuyue:{}",this.getClass().getName(),shiwuYuyue.toString());
        ShiwuYuyueEntity oldShiwuYuyueEntity = shiwuYuyueService.selectById(shiwuYuyue.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            shiwuYuyue.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        if("".equals(shiwuYuyue.getShiwuYuyueText()) || "null".equals(shiwuYuyue.getShiwuYuyueText())){
                shiwuYuyue.setShiwuYuyueText(null);
        }
        if("".equals(shiwuYuyue.getShiwuYuyuePhoto()) || "null".equals(shiwuYuyue.getShiwuYuyuePhoto())){
                shiwuYuyue.setShiwuYuyuePhoto(null);
        }
        if("".equals(shiwuYuyue.getShiwuYuyueYesnoText()) || "null".equals(shiwuYuyue.getShiwuYuyueYesnoText())){
                shiwuYuyue.setShiwuYuyueYesnoText(null);
        }

            shiwuYuyueService.updateById(shiwuYuyue);//根据id更新
            return R.ok();
    }


    /**
    * 审核
    */
    @RequestMapping("/shenhe")
    public R shenhe(@RequestBody ShiwuYuyueEntity shiwuYuyueEntity, HttpServletRequest request){
        logger.debug("shenhe方法:,,Controller:{},,shiwuYuyueEntity:{}",this.getClass().getName(),shiwuYuyueEntity.toString());

        ShiwuYuyueEntity oldShiwuYuyue = shiwuYuyueService.selectById(shiwuYuyueEntity.getId());//查询原先数据

        if(shiwuYuyueEntity.getShiwuYuyueYesnoTypes() == 2){//通过
            ShiwuEntity shiwuEntity = shiwuService.selectById(oldShiwuYuyue.getShiwuId());
            shiwuEntity.setShangxiaTypes(2);
            shiwuService.updateById(shiwuEntity);
//            shiwuYuyueEntity.setShiwuYuyueTypes();
        }else if(shiwuYuyueEntity.getShiwuYuyueYesnoTypes() == 3){//拒绝
//            shiwuYuyueEntity.setShiwuYuyueTypes();
        }
        shiwuYuyueEntity.setShiwuYuyueShenheTime(new Date());//审核时间
        shiwuYuyueService.updateById(shiwuYuyueEntity);//审核

        return R.ok();
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<ShiwuYuyueEntity> oldShiwuYuyueList =shiwuYuyueService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        shiwuYuyueService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //.eq("time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
        try {
            List<ShiwuYuyueEntity> shiwuYuyueList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            ShiwuYuyueEntity shiwuYuyueEntity = new ShiwuYuyueEntity();
//                            shiwuYuyueEntity.setShiwuYuyueUuidNumber(data.get(0));                    //报名编号 要改的
//                            shiwuYuyueEntity.setShiwuId(Integer.valueOf(data.get(0)));   //寻物启示 要改的
//                            shiwuYuyueEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            shiwuYuyueEntity.setShiwuYuyueText(data.get(0));                    //报名理由 要改的
//                            shiwuYuyueEntity.setShiwuYuyuePhoto("");//详情和图片
//                            shiwuYuyueEntity.setInsertTime(date);//时间
//                            shiwuYuyueEntity.setShiwuYuyueYesnoTypes(Integer.valueOf(data.get(0)));   //报名状态 要改的
//                            shiwuYuyueEntity.setShiwuYuyueYesnoText(data.get(0));                    //审核回复 要改的
//                            shiwuYuyueEntity.setShiwuYuyueShenheTime(sdf.parse(data.get(0)));          //审核时间 要改的
//                            shiwuYuyueEntity.setCreateTime(date);//时间
                            shiwuYuyueList.add(shiwuYuyueEntity);


                            //把要查询是否重复的字段放入map中
                                //报名编号
                                if(seachFields.containsKey("shiwuYuyueUuidNumber")){
                                    List<String> shiwuYuyueUuidNumber = seachFields.get("shiwuYuyueUuidNumber");
                                    shiwuYuyueUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> shiwuYuyueUuidNumber = new ArrayList<>();
                                    shiwuYuyueUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("shiwuYuyueUuidNumber",shiwuYuyueUuidNumber);
                                }
                        }

                        //查询是否重复
                         //报名编号
                        List<ShiwuYuyueEntity> shiwuYuyueEntities_shiwuYuyueUuidNumber = shiwuYuyueService.selectList(new EntityWrapper<ShiwuYuyueEntity>().in("shiwu_yuyue_uuid_number", seachFields.get("shiwuYuyueUuidNumber")));
                        if(shiwuYuyueEntities_shiwuYuyueUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(ShiwuYuyueEntity s:shiwuYuyueEntities_shiwuYuyueUuidNumber){
                                repeatFields.add(s.getShiwuYuyueUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [报名编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        shiwuYuyueService.insertBatch(shiwuYuyueList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = shiwuYuyueService.queryPage(params);

        //字典表数据转换
        List<ShiwuYuyueView> list =(List<ShiwuYuyueView>)page.getList();
        for(ShiwuYuyueView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Integer id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ShiwuYuyueEntity shiwuYuyue = shiwuYuyueService.selectById(id);
            if(shiwuYuyue !=null){


                //entity转view
                ShiwuYuyueView view = new ShiwuYuyueView();
                BeanUtils.copyProperties( shiwuYuyue , view );//把实体数据重构到view中

                //级联表
                    ShiwuEntity shiwu = shiwuService.selectById(shiwuYuyue.getShiwuId());
                if(shiwu != null){
                    BeanUtils.copyProperties( shiwu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setShiwuId(shiwu.getId());
                }
                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(shiwuYuyue.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody ShiwuYuyueEntity shiwuYuyue, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,shiwuYuyue:{}",this.getClass().getName(),shiwuYuyue.toString());
        Wrapper<ShiwuYuyueEntity> queryWrapper = new EntityWrapper<ShiwuYuyueEntity>()
            .eq("shiwu_yuyue_uuid_number", shiwuYuyue.getShiwuYuyueUuidNumber())
            .eq("shiwu_id", shiwuYuyue.getShiwuId())
            .eq("yonghu_id", shiwuYuyue.getYonghuId())
            .eq("shiwu_yuyue_text", shiwuYuyue.getShiwuYuyueText())
            .in("shiwu_yuyue_yesno_types", new Integer[]{1,2})
            .eq("shiwu_yuyue_yesno_text", shiwuYuyue.getShiwuYuyueYesnoText())
//            .notIn("shiwu_yuyue_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShiwuYuyueEntity shiwuYuyueEntity = shiwuYuyueService.selectOne(queryWrapper);
        if(shiwuYuyueEntity==null){
            shiwuYuyue.setInsertTime(new Date());
            shiwuYuyue.setShiwuYuyueYesnoTypes(1);
            shiwuYuyue.setCreateTime(new Date());
        shiwuYuyueService.insert(shiwuYuyue);

            return R.ok();
        }else {
            if(shiwuYuyueEntity.getShiwuYuyueYesnoTypes()==1)
                return R.error(511,"有相同的待审核的数据");
            else if(shiwuYuyueEntity.getShiwuYuyueYesnoTypes()==2)
                return R.error(511,"有相同的审核通过的数据");
            else
                return R.error(511,"表中有相同数据");
        }
    }

}

