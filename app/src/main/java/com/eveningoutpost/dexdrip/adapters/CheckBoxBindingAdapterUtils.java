package com.eveningoutpost.dexdrip.adapters;

import android.widget.CompoundButton;
import android.widget.CompoundButton.*;

import androidx.databinding.*;


@InverseBindingMethods(@InverseBindingMethod(type = CompoundButton.class, attribute = "checked", method = "getChecked"))
public class CheckBoxBindingAdapterUtils {

	@BindingAdapter("checked")
	public static void setChecked(CompoundButton view, Boolean checked) {
		if (checked == null) checked = false;
		setChecked(view, (boolean) checked);
	}

	@BindingAdapter("checked")
	public static void setChecked(CompoundButton view, boolean checked) {
		if (view.isChecked() != checked) {
			view.setChecked(checked);
		}
	}

	@InverseBindingAdapter(attribute = "checked")
	public static boolean getChecked(CompoundButton button) {
		return button.isChecked();
	}

	@BindingAdapter(value = {"onCheckedChanged", "checkedAttrChanged"},
			requireAll = false)
	public static void setListeners(CompoundButton view, final OnCheckedChangeListener listener,
	                                final InverseBindingListener attrChange) {
		if (attrChange == null) {
			view.setOnCheckedChangeListener(listener);
		} else {
			view.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if (listener != null) {
					listener.onCheckedChanged(buttonView, isChecked);
				}
				attrChange.onChange();
			});
		}
	}

}
