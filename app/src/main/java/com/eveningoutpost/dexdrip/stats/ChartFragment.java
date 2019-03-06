package com.eveningoutpost.dexdrip.stats;

import android.os.*;
import android.view.*;

import androidx.annotation.*;
import androidx.fragment.app.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.models.UserError.*;

/**
 * Created by adrian on 30/06/15.
 */
public class ChartFragment extends Fragment {

    private ChartView chartView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        UserError.Log.i("DrawStats", "ChartFragment onCreateView");
        return getView();
    }

    @Nullable
    @Override
    public View getView() {
        UserError.Log.i("DrawStats", "getView - ChartFragment");

        if (chartView == null) {
            chartView = new ChartView(getActivity().getApplicationContext());
            chartView.setTag(1);
        }
        return chartView;
    }
}
