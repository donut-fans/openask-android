package fans.openask.encode;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.common.collect.Lists;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import fans.openask.utils.spring.UriUtils;

public final class QSV2 {

    private QSV2() {
    }

    private static final class QsHolder {
        private static final QSV2 QSV_1 = new QSV2();
    }

    public static QSV2 en() {
        return QsHolder.QSV_1;
    }

    private boolean isPrimitiveOrWrapperType(Object obj) throws IllegalAccessException {
        if (obj.getClass().isPrimitive()) {
            return true;
        } else {
            Field type = ReflectUtil.getField(obj.getClass(), "TYPE");
            if (type != null) {
                return ((Class<?>) type.get(null)).isPrimitive();
            }
            return false;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String _stringify(Object obj, String prefix, Map<String, String> map) throws NoSuchFieldException, IllegalAccessException {
//        if (!map.containsKey(prefix.toLowerCase())){
//            List<String> keyParts = StrUtil.split(prefix, ".");
//            List<String> collect = keyParts.stream().map(key -> {
//                if (map.containsKey(key)) {
//                    return map.get(key);
//                }
//                return key;
//            }).collect(Collectors.toList());
//            map.put(prefix.toLowerCase(), StrUtil.join(".",collect));
//        }
//        prefix = prefix.toLowerCase();
//        if (ObjectUtil.isNull(obj)) {
//            return prefix + "=";
//        }
//        if (isPrimitiveOrWrapperType(obj) || obj instanceof String || obj instanceof BigDecimal) {
//            try {
//                return prefix + "=" + URLEncoder.encode(obj.toString(), "utf-8");
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        }
//        if (obj instanceof Map) {
//            List<String> retStr = new ArrayList<>();
//            for (Object key : ((Map) obj).keySet()) {
//                String stringify = this._stringify(((Map) obj).get(key), prefix + "." + key, map);
//                if (StrUtil.isEmpty(stringify)) continue;
//                retStr.add(stringify);
//            }
//            retStr = retStr.stream().sorted().collect(Collectors.toList());
//            return join(retStr);
//        } else if (obj instanceof List) {
//            List<?> list = (List<?>) obj;
//            List<String> retStr = new ArrayList<>();
//            for (int key = 0; key < list.size(); key++) {
//                String stringify = _stringify(((List<?>) obj).get(key), prefix, map);
//                if (StrUtil.isEmpty(stringify)) continue;
//                retStr.add(stringify);
//            }
//            return join(retStr);
//        }
//        return "";

        if (!map.containsKey(prefix.toLowerCase())){
            String prefixStr = StrUtil.subBefore(prefix, ".", true);
            String sufferStr = StrUtil.subAfter(prefix, ".", true);
            List<String> keyParts = Lists.newArrayList(prefixStr,sufferStr).stream().filter(StrUtil::isNotEmpty).collect(Collectors.toList());//StrUtil.split(prefix, ".");
            List<String> collect = keyParts.stream().map(key -> {
                if (map.containsKey(key)) {
                    return map.get(key);
                }
                return key;
            }).collect(Collectors.toList());
            map.put(prefix.toLowerCase(), StrUtil.join(".",collect));
        }
        prefix = prefix.toLowerCase();
        if (ObjectUtil.isNull(obj)) {
            return prefix + "=";
        }
        if (isPrimitiveOrWrapperType(obj) || obj instanceof String || obj instanceof BigDecimal) {
            try {
                return prefix + "=" + UriUtils.encode(obj.toString(), "utf-8");
            } catch (UnsupportedEncodingException e) {
//                throw new RuntimeException(e);
                e.printStackTrace();
                return prefix + "=" + obj.toString();
            }
        }
        if (obj instanceof Map) {
            List<String> retStr = new ArrayList<>();
            for (Object key : ((Map) obj).keySet()) {
                String stringify = this._stringify(((Map) obj).get(key), prefix + "." + key, map);
                if (StrUtil.isEmpty(stringify)) continue;
                retStr.add(stringify);
            }
            retStr = retStr.stream().sorted().collect(Collectors.toList());
            return join(retStr);
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            List<String> retStr = new ArrayList<>();
            for (int key = 0; key < list.size(); key++) {
                String stringify = _stringify(((List<?>) obj).get(key), prefix, map);
                if (StrUtil.isEmpty(stringify)) continue;
                retStr.add(stringify);
            }
            return join(retStr);
        }
        return "";
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String stringifyObj(Object obj) throws NoSuchFieldException, IllegalAccessException {

        if (obj instanceof Map) {
            return stringify((Map<String, ?>) obj);
        } else {
            return stringify((List<?>) obj);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String stringify(Map<String, ?> obj) throws NoSuchFieldException, IllegalAccessException {

        Map<String,String> map = new HashMap<>();
        List<String> keys = new ArrayList<>();
        for (Object key : ((Map) obj).keySet()) {
            String stringify = this._stringify(((Map<?, ?>) obj).get(key), (String) key,map);
            if (StrUtil.isEmpty(stringify)) continue;
            keys.add(stringify);
        }
        System.out.println(map);
        keys = keys.stream().sorted().collect(Collectors.toList());
        keys =  origin(keys,map);
        return join(keys);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private List<String> origin(List<String> keys, Map<String,String> map){
        return  keys.stream().map(key -> {
            List<String> split = StrUtil.split(key, "&");
            List<String> newList = new ArrayList<>();
            for (String s : split) {
                String[] kv = s.split("=");
                String k = kv[0];
                String originKey = map.get(k);
                kv[0] = originKey;
                String newStr = kv[0] + "=";
                if (kv.length > 1){
                    newStr = newStr + kv[1];
                }
                newList.add(newStr);
            }
            return StrUtil.join("&",newList);
        }).collect(Collectors.toList());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String stringify(List<?> objs) throws NoSuchFieldException, IllegalAccessException {

        Map<String,String> map = new HashMap<>();
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < objs.size(); i++) {
            String stringify = this._stringify(objs.get(i), i + "",map);
            if (StrUtil.isEmpty(stringify)) continue;
            keys.add(stringify);
        }
        keys =  origin(keys,map);
//        keys = keys.stream().sorted().collect(Collectors.toList());
        return join(keys);
    }

    private static String join(Collection<?> values) {
        return CollectionUtil.join(values, "&");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        QSV2 QSV2 = new QSV2();

        String str = "{\n" +
                "    \"createdTime\": \"1662024714889\",\n" +
                "    \"shareUrl\": \"https:\\/\\/h5.donut.fans:81\\/#\\/circleDetails?params=1564088836671713281-1565271220217745408-question\",\n" +
                "    \"id\": \"1565271220217745408\",\n" +
                "    \"likeCnt\": \"0\",\n" +
                "    \"like\": false,\n" +
                "    \"totalPage\": 0,\n" +
                "    \"anonymity\": true@123.com,\n" +
                "    \"top\": false,\n" +
                "    \"currentUser\": {\n" +
                "        \"headIcon\": \"https:\\/\\/donut-test01.oss-us-west-1.aliyuncs.com\\/00f4c8dd8fde48c98eea2908bbb49d4d\\/1661143538000_ios.png\",\n" +
                "        \"id\": \"758912e3483e736c75bfc2fa7b6b33f0\",\n" +
                "        \"like\": false,\n" +
                "        \"username\": \"632375960eeqr\",\n" +
                "        \"totalPage\": 0,\n" +
                "        \"role\": 2\n" +
                "    },\n" +
                "    \"type\": 2,\n" +
                "    \"isAnswer\": false,\n" +
                "    \"shareTitle\": \"Ceshiceshi\",\n" +
                "    \"poster\": {\n" +
                "        \"headIcon\": \"https:\\/\\/donut-test01.oss-us-west-1.aliyuncs.com\\/00f4c8dd8fde48c98eea2908bbb49d4d\\/1661143538000_ios.png\",\n" +
                "        \"id\": \"758912e3483e736c75bfc2fa7b6b33f0\",\n" +
                "        \"like\": false,\n" +
                "        \"username\": \"632375960eeqr\",\n" +
                "        \"totalPage\": 0,\n" +
                "        \"role\": 2\n" +
                "    },\n" +
                "    \"picUrls\": [],\n" +
                "    \"circleName\": \"测试官方圈子\",\n" +
                "    \"circleId\": \"1564088836671713281\",\n" +
                "    \"content\": \"Ceshiceshi\",\n" +
                "    \"showAll\": false,\n" +
                "    \"shareParams\": {\n" +
                "        \"title\": \"Ceshiceshi\",\n" +
                "        \"url\": \"https:\\/\\/h5.donut.fans:81\\/#\\/circleDetails?params=1564088836671713281-1565271220217745408-question\",\n" +
                "        \"images\": \"https:\\/\\/donut-test01.oss-us-west-1.aliyuncs.com\\/00f4c8dd8fde48c98eea2908bbb49d4d\\/1661143538000_ios.png\",\n" +
                "        \"text\": \"This is an interesting issue that everyone is discussing, and I invite you to join us\"\n" +
                "    }\n" +
                "}";
//        str = "{\"files\":[{\"Name\":\"1665631531000_0_46c7acbaf29f7ee8_ios.png\"},{\"Name\":\"1665631531000_1_46c7acbaf29f7ee8_ios.png\"},{\"Name\":\"1665631531000_2_46c7acbaf29f7ee8_ios.png\"}],\"temp\":0}";

        str = "{\n" +
                " payMethodId: 4,\n" +
                " amount: 10,\n" +
                " paymentAddress: '0x42E3C567b9DE17D7FdE66a011AE8FC9B12ABf155',\n" +
                " verifyCode: '8888',\n" +
                "}";

//        Map map = JSONUtil.parseObj(str);
//        String stringify = QSV2.stringify(map);
//        System.out.println(stringify);

    }
}
