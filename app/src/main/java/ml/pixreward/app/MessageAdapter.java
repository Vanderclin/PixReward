package ml.pixreward.app;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;


public class MessageAdapter extends ArrayAdapter<Message> {
    private Activity context;
    private List<Message> Messages;

    public MessageAdapter(Activity context, List<Message> Messages) {
        super(context, R.layout.card_message, Messages);
        this.context = context;
        this.Messages = Messages;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View listViewItem = inflater.inflate(R.layout.card_message, null, true);
        TextView textViewName = listViewItem.findViewById(R.id.display_username);
        TextView textDisplayMessage = (TextView) listViewItem.findViewById(R.id.display_message);
        TextView textDisplayTime = (TextView) listViewItem.findViewById(R.id.display_time);


        Message Message = Messages.get(position);
        textViewName.setText(Message.getNAME());
        textDisplayMessage.setText(Message.getMESSAGE());
        textDisplayTime.setText(Message.getTIME());

        return listViewItem;
    }
}
