package com.example.bledemo_v50;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * Created by 贲 on 2016/8/29.
 */
public class DataAdapter extends BaseAdapter {
    private List<Map<String, Object>> data;
    private LayoutInflater layoutInflater;
    private Context context;

    public DataAdapter(Context context, List<Map<String, Object>> data) {
        this.context = context;
        this.data = data;
        this.layoutInflater = LayoutInflater.from(context);
    }

    /*
    组件
     */
    class Component {
        private TextView View_time;
        private TextView View_major;
        private TextView View_minor;
        private TextView View_distance;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Component component = null;
        if (convertView == null) {
            component = new Component();
            //获得组件，实例化组件
            convertView = layoutInflater.inflate(R.layout.list_item_data, null);
            component.View_time = (TextView) convertView.findViewById(R.id.time);
            component.View_major = (TextView) convertView.findViewById(R.id.major);
            component.View_minor = (TextView) convertView.findViewById(R.id.minor);
            component.View_distance = (TextView) convertView.findViewById(R.id.distance);
            convertView.setTag(component);
        } else {
            component = (Component) convertView.getTag();
        }
        //绑定数据
        component.View_time.setText((String) data.get(position).get("time"));
        component.View_major.setText((String) data.get(position).get("major"));
        component.View_minor.setText((String) data.get(position).get("minor"));
        component.View_distance.setText((String) data.get(position).get("distance"));

        return convertView;
    }
}
