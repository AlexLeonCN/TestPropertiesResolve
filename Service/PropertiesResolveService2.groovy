import java.text.SimpleDateFormat

class PropertiesResolveService2 {
    final String filePath = "../Properties/Test.properties"
    PropertiesConfiguration proConf = new PropertiesConfiguration(filePath)

    /*获取properties的map*/
    def getPropertiesByMap(){
        def resultMap = [:]
        try{
            Iterator<String> iterator = proConf.getkeys()
            def data = [:]
            while(iterator.hasNext()){
                def key = iterator.next()
                String val = proConf.getProperty(key)
                if (val.contains("[") && val.contains("]")){
                    val = val.substring(1,val.length()-1)
                }
                data.put(key,val)
            }
            resultMap.put("code","200")
            resultMap.put("msg","success")
            resultMap.put("data",data)
        }catch(Exception e){
            e.printStackTrace()
            resultMap.put("code","201")
            resultMap.put("msg","获取失败")
        }
        return resultMap
    }

    /*更新Properties元素*/
    def updateProperties(Map map){
        def resultMap = [:]
        //前置校验
        if (map == null || map.isEmpty()){
            resultMap = ["code":"201","msg":"参数不能为空"]
        }else{
            map.each {
                if (it.key == null || "".equals(it.key) || it.value == null || "".equals(it.value)){
                    resultMap = ["code":"201","msg":"元素项不能为空"]
                    return
                }
            }
        }
        if (!resultMap.isEmpty()){
            return resultMap
        }
        try{
            copyProperties()
            map.each {
                def key = it.key as String
                def val = it.value as String
                if (",".equals(val[val.length()-1])){//防止有人在最后一位多输入一个逗号
                    val = val.substring(0,val.length()-1)
                }
                proConf.clearProperty(key)
                proConf.setProperty(key,val)
                proConf.save()
            }
            resultMap.put("code":"200","msg":"success")
        }catch(Exception e){
            e.printStackTrace()
            resultMap.put("code","201")
            resultMap.put("msg","更新失败")
        }
        return resultMap
    }

    /*删除Properties元素*/
    def deleteProperty(def key){
        def resultMap = [:]
        try{
            copyProperties()
            proConf.clearProperty(key)
            proConf.save()
            resultMap.put("code":"200","msg":"success")
        }catch(Exception e){
            e.printStackTrace()
            resultMap.put("code":"201","msg":"删除失败")
        }
        return resultMap
    }

    /*备份properties文件*/
    def copyProperties(){
        InputStream fis = null
        OutputStream fos = null
        //宿主机有时差,需要多加8小时
        long rightTime = (long)((new Date()).getTime() + 8*60*60*1000)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss")
        def newFilePath  = filePath + ".copy" + sdf.format(rightTime)//新的绝对路径

        try{
            fis = new FileInputStream(new File(filePath))
            fos = new FileOutputStream(new File(newFilePath))
            byte[] bytes = new byte[1024]
            def len = -1
            while (len = fis.read(bytes) != -1){
                fos.write(bytes,0,len)
            }
            return  newFilePath
        }catch(Exception e){
            e.printStackTrace()
        }finally{
            closeFileInputStream(fis as FileInputStream)
            closeFileOutputStream(fos as FileOutputStream)
        }
    }

    //关闭输入流
    void closeFileInputStream(FileInputStream fis){
        if (fis != null)
            try{
                fis.close()
            }catch(Exception e){
                e.printStackTrace()
            }
    }

    //关闭输出流
    void closeFileOutputStream(FileOutputStream fos){
        if (fos != null)
            try{
                fos.close()
            }catch(Exception e){
                e.printStackTrace()
            }
    }

}