package ml.pixreward.app;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;



public class RankingAdapter extends ArrayAdapter<Ranking> {
    private Activity context;
    List<Ranking> rankingList;

    public RankingAdapter(Activity context, List<Ranking> productsList) {
        super(context, R.layout.card_ranking, productsList);
        this.context = context;
        this.rankingList = productsList;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View listViewItem = inflater.inflate(R.layout.card_ranking, null, true);

        TextView rankingName = listViewItem.findViewById(R.id.rankingUsername);
		TextView rankingPoints = listViewItem.findViewById(R.id.rankingPoints);
		TextView rankingBalance = listViewItem.findViewById(R.id.rankingBalance);
        Ranking mRanking = rankingList.get(position);
		String replaceValue = mRanking.getPoints().toString().replaceAll("[$,.]", "");
		double doubleValue = Double.parseDouble(replaceValue);
		String points = Integer.toString(mRanking.getPoints());
		String balance = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format((doubleValue / 1000));
		
        rankingName.setText(mRanking.getName());
		rankingPoints.setText(points);
		rankingBalance.setText(balance);

        return listViewItem;
    }
}
