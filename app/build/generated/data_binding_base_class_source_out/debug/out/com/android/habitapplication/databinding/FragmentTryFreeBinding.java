// Generated by view binder compiler. Do not edit!
package com.android.habitapplication.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.android.habitapplication.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentTryFreeBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final ImageView checkmark1;

  @NonNull
  public final ImageView checkmark2;

  @NonNull
  public final ImageView checkmark3;

  @NonNull
  public final TextView describe1;

  @NonNull
  public final TextView describe2;

  @NonNull
  public final TextView describe3;

  @NonNull
  public final ImageView discountIv;

  @NonNull
  public final TextView discountTv;

  @NonNull
  public final TextView expireTv;

  @NonNull
  public final TextView mostTv;

  @NonNull
  public final TextView price1;

  @NonNull
  public final TextView price2;

  @NonNull
  public final TextView price3;

  @NonNull
  public final CardView priceCard1;

  @NonNull
  public final CardView priceCard2;

  @NonNull
  public final CardView priceCard3;

  @NonNull
  public final TextView unlockTv;

  private FragmentTryFreeBinding(@NonNull LinearLayout rootView, @NonNull ImageView checkmark1,
      @NonNull ImageView checkmark2, @NonNull ImageView checkmark3, @NonNull TextView describe1,
      @NonNull TextView describe2, @NonNull TextView describe3, @NonNull ImageView discountIv,
      @NonNull TextView discountTv, @NonNull TextView expireTv, @NonNull TextView mostTv,
      @NonNull TextView price1, @NonNull TextView price2, @NonNull TextView price3,
      @NonNull CardView priceCard1, @NonNull CardView priceCard2, @NonNull CardView priceCard3,
      @NonNull TextView unlockTv) {
    this.rootView = rootView;
    this.checkmark1 = checkmark1;
    this.checkmark2 = checkmark2;
    this.checkmark3 = checkmark3;
    this.describe1 = describe1;
    this.describe2 = describe2;
    this.describe3 = describe3;
    this.discountIv = discountIv;
    this.discountTv = discountTv;
    this.expireTv = expireTv;
    this.mostTv = mostTv;
    this.price1 = price1;
    this.price2 = price2;
    this.price3 = price3;
    this.priceCard1 = priceCard1;
    this.priceCard2 = priceCard2;
    this.priceCard3 = priceCard3;
    this.unlockTv = unlockTv;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentTryFreeBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentTryFreeBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_try_free, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentTryFreeBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.checkmark1;
      ImageView checkmark1 = ViewBindings.findChildViewById(rootView, id);
      if (checkmark1 == null) {
        break missingId;
      }

      id = R.id.checkmark2;
      ImageView checkmark2 = ViewBindings.findChildViewById(rootView, id);
      if (checkmark2 == null) {
        break missingId;
      }

      id = R.id.checkmark3;
      ImageView checkmark3 = ViewBindings.findChildViewById(rootView, id);
      if (checkmark3 == null) {
        break missingId;
      }

      id = R.id.describe1;
      TextView describe1 = ViewBindings.findChildViewById(rootView, id);
      if (describe1 == null) {
        break missingId;
      }

      id = R.id.describe2;
      TextView describe2 = ViewBindings.findChildViewById(rootView, id);
      if (describe2 == null) {
        break missingId;
      }

      id = R.id.describe3;
      TextView describe3 = ViewBindings.findChildViewById(rootView, id);
      if (describe3 == null) {
        break missingId;
      }

      id = R.id.discount_iv;
      ImageView discountIv = ViewBindings.findChildViewById(rootView, id);
      if (discountIv == null) {
        break missingId;
      }

      id = R.id.discount_tv;
      TextView discountTv = ViewBindings.findChildViewById(rootView, id);
      if (discountTv == null) {
        break missingId;
      }

      id = R.id.expire_tv;
      TextView expireTv = ViewBindings.findChildViewById(rootView, id);
      if (expireTv == null) {
        break missingId;
      }

      id = R.id.most_tv;
      TextView mostTv = ViewBindings.findChildViewById(rootView, id);
      if (mostTv == null) {
        break missingId;
      }

      id = R.id.price1;
      TextView price1 = ViewBindings.findChildViewById(rootView, id);
      if (price1 == null) {
        break missingId;
      }

      id = R.id.price2;
      TextView price2 = ViewBindings.findChildViewById(rootView, id);
      if (price2 == null) {
        break missingId;
      }

      id = R.id.price3;
      TextView price3 = ViewBindings.findChildViewById(rootView, id);
      if (price3 == null) {
        break missingId;
      }

      id = R.id.price_card1;
      CardView priceCard1 = ViewBindings.findChildViewById(rootView, id);
      if (priceCard1 == null) {
        break missingId;
      }

      id = R.id.price_card2;
      CardView priceCard2 = ViewBindings.findChildViewById(rootView, id);
      if (priceCard2 == null) {
        break missingId;
      }

      id = R.id.price_card3;
      CardView priceCard3 = ViewBindings.findChildViewById(rootView, id);
      if (priceCard3 == null) {
        break missingId;
      }

      id = R.id.unlock_tv;
      TextView unlockTv = ViewBindings.findChildViewById(rootView, id);
      if (unlockTv == null) {
        break missingId;
      }

      return new FragmentTryFreeBinding((LinearLayout) rootView, checkmark1, checkmark2, checkmark3,
          describe1, describe2, describe3, discountIv, discountTv, expireTv, mostTv, price1, price2,
          price3, priceCard1, priceCard2, priceCard3, unlockTv);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
