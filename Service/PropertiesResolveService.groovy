import java.text.SimpleDateFormat
class PropertiesResolveService {
    final String filePath = "../Properties/Test.properties"
    def props = new Properties()
    /*获取properties的map*/
    def getPropertiesByMap() {
        def resultMap = [:]//前端返回
        def data = [:]
        InputStream fis = null
        try{
            fis = new FileInputStream(filePath)
            props.load(fis)
            Iterator<String> it = props.stringPropertyNames().iterator()
            while(it.hasNext()){
                String key = it.next()
                def val = props[key]
                data.put(key,val)
            }
            resultMap.put("code","200")
            resultMap.put("msg","success")
            resultMap.put("data",data)
        }catch(Exception e){
            resultMap.put("code","201")
            resultMap.put("msg","获取失败")
        }finally {
            closeFileInputStream(fis as FileInputStream)
        }
        return resultMap
    }

    /*根据map更新properties, 可以传单个key:val,也可以传多个
    * 该properties文件中没有该key则新增,如果已有则更新*/
    def updateProperties(Map map){
        def result = [:]
        try{
            if (map != null && !map.isEmpty()){
                //进行备份
                copyProperties()
                map.each {
                    updateProperty(it.key as String, it.value as String)
                }
            }
            resetFile()
            result.put("code","200")
            result.put("msg","success")
        }catch(Exception e){
            e.printStackTrace()
            result.put("code","201")
            result.put("msg","更新失败")
        }
        return result
    }

    def updateProperty(String key, String val){
        InputStream fis = null
        OutputStream fos = null
        try{
            fis = new FileInputStream(filePath)
            props.load(fis)
            fos = new FileOutputStream(filePath)
            props.setProperty(key,val)
            props.store(fos,"update '$key' value")
        }catch(Exception e){
            e.printStackTrace()
            throw new Exception("更新Property失败")
        }finally{
            closeFileInputStream(fis as FileInputStream)
            closeFileOutputStream(fos as FileOutputStream)
        }
    }

    /*通过key删除val*/
    def deleteProperty(def key){
        def result = [:]
        InputStream fis = null
        OutputStream fos = null
        try{
            copyProperties()
            fis = new FileInputStream(filePath)
            props.load(fis)
            fos = new FileOutputStream(filePath)
            props.remove(key)
            props.store(fos,"update '$key' value")
            resetFile()
            result.put("code","200")
            result.put("msg","success")
        }catch(Exception e){
            e.printStackTrace()
            result.put("code","201")
            result.put("msg","删除失败")
        }finally{
            closeFileInputStream(fis as FileInputStream)
            closeFileOutputStream(fos as FileOutputStream)
        }
        return result
    }

    /*备份properties文件*/
    def copyProperties(){
        InputStream fis = null
        OutputStream fos = null
        Date d = new Date()
        //宿主机有时差,需要多加8小时
        long rightTime = (long)(d.getTime() + 8*60*60*1000)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss")
        def newFilePath = filePath + ".copy" + sdf.format(rightTime)

        try {
            fis = new FileInputStream(new File(filePath))
            fos = new FileOutputStream(new File(newFilePath))
            byte[] bytes = new byte[1024]
            def len = -1
            while ((len = fis.read(bytes)) != -1){
                fos.write(bytes,0,len)
            }
            return newFilePath
        }catch(Exception e){
            e.printStackTrace()
        }finally{
            closeFileInputStream(fis as FileInputStream)
            closeFileOutputStream(fos as FileOutputStream)
        }
    }

    /*重置文件格式[重要]*/
    void resetFile(){
        InputStream fisData = null
        InputStream fis = null
        OutputStream fos = null
        OutputStream fosAppend = null
        OutputStreamWriter osw = null
        //读取properties文件的元素,生成dataMap
        try {
            def dataMap = [:]
            fisData = new FileInputStream(filePath)
            props.load(fisData)
            Iterator<String> it = props.stringPropertyNames().iterator()
            while (it.hasNext()) {
                String key = it.next()
                def val = props[key]
                dataMap.put(key, val)
            }
            //遍历map将非特殊换行格式的元素key值记录在keyList中
            def keyList = []
            dataMap.each {
                if (!(it.value as String).contains("=") && !(it.value as String).contains(",")){
                    keyList << it.key
                }
            }
            //将所有非特殊换行格式的元素从map中剔除
            keyList.each{
                dataMap.remove(it)
            }
            //遍历map,先将特殊换行格式元素统一一次性删除
            dataMap.each {
                //删除该元素
                fis = new FileInputStream(filePath)
                props.load(fis)
                fos = new FileOutputStream(filePath)
                props.remove(it.key)
                props.store(fos,"update '$it.key' value")
            }
            //将特殊换行格式的元素value分情况进行处理,一共三种情况,代码最后有说明
            File file = new File(filePath)
            fosAppend = new FileOutputStream(file,true)
            osw = new OutputStreamWriter(fosAppend)
            dataMap.each {
                if ((it.value as String).contains(",") && (it.value as String).contains("=")) {
                    //流追加文本的操作
                    osw.write(it.key + "=\\")
                    osw.write("\r\n")
                    String[] strArr = it.value.split(",")
                    (strArr.length - 1).times {//最后一个元素后面不加\
                        osw.write(strArr[it] + ",\\")
                        osw.write("\r\n")
                    }
                    osw.write(strArr[strArr.length - 1])//单独添加最后一个元素
                    osw.write("\r\n")
                }
                if ((it.value as String).contains(",") && !(it.value as String).contains("=")) {
                    //流追加文本的操作
                    osw.write(it.key + "=")
                    String[] strArr = it.value.split(",")
                    (strArr.length - 1).times {//最后一个元素后面不加\
                        osw.write(strArr[it] + ",\\")
                        osw.write("\r\n")
                    }
                    osw.write(strArr[strArr.length - 1])//单独添加最后一个元素
                    osw.write("\r\n")
                }
                if (!(it.value as String).contains(",") && (it.value as String).contains("=")) {
                    //流追加文本的操作
                    osw.write(it.key + "=\\")
                    osw.write("\r\n")
                    osw.write(it.value)
                    osw.write("\r\n")
                }
            }
        }catch(Exception e){
            e.printStackTrace()
        }finally{
            closeOutputStreamWriter(osw)
            closeFileInputStream(fis as FileInputStream)
            closeFileInputStream(fisData as FileInputStream)
            closeFileOutputStream(fos as FileOutputStream)
            closeFileOutputStream(fosAppend as FileOutputStream)
        }
    }

    /*关闭输入流*/
    void closeFileInputStream(FileInputStream fis) {
        try{
            if (fis != null) {
                fis.close()
            }
        }catch(Exception e){
            e.printStackTrace()
        }
    }
    /*关闭输出流*/
    void closeFileOutputStream(FileOutputStream fos) {
        try{
            if (fos != null) {
                fos.close()
            }
        }catch(Exception e){
            e.printStackTrace()
        }
    }
    /*关闭输入流*/
    void closeOutputStreamWriter(OutputStreamWriter osw) {
        try{
            if (osw != null) {
                osw.close()
            }
        }catch(Exception e){
            e.printStackTrace()
        }
    }
}

/*
c-liangjr 2020-09-05
本说明注释建议保留
我们的properties中含有特殊换行格式的元素
而Properties对象在更新和删除元素时使用的store()方法,会将这些特殊换行格式重置为普通格式
也就是说如果先进行格式转换, 后续对其他元素再进行操作时,又会将已经格式转换好的元素重置为普通格式
因此需要在每次新增\更新\删除操作之后,最后调用本类的resetFile()方法统一处理, 将文件恢复成我们需要的换行格式

目前有三种情况(以后还有其他情况再补充)
resetFile()方法中对这三种情况分别作了处理:

1.元素value既包含逗号又包含等号
如:
rate.concurrent.filter.url-filter.config=\
/ecam/sms/get=100,\
/ecam/page/register/c1634e=3,\
/ecam/page/156576767676473/c1634e=3,\
/ecam/page/receiveCompon/c1634e=3

2.元素value只包含逗号不包含等号
如:
rate.concurrent.filter.metric-filter.whitelistconfig=/mspmk-web-p2p/zh_CN/static/Supplier/zepto.min.js,\
/msper-web-test/getUsers

3.元素value只包含等号不包含逗号(即一个元素值但是换行了)
如:
test.blabla.config=\
/sadf/asdf/asdfasdf/asdfasd=110
 */